//
//  CTRNNWrapper.java
//  CTRNNWrapper
//
//  Created by iroro on 25/06/2010.
//  Copyright (c) 2010-2012 de'fchild productions. All rights reserved.
//

import com.cycling74.max.*;

// i/o
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;



public class CTRNNWrapper extends MaxObject
{
	// Timing
	MaxClock mClock;
	MaxQelem mQelem;

	private double mTickPeriod;  // in milliseconds
	private double[] mInput;


	// Neural Network & state
	private CTRNN mCTRNN;			
	private CTRNNSettings mSettings;
	private int mCircuitSize;


	// Numerical Integration technique
	public enum NumericalIntegration { RUNGEKUTTA, EULER };
	private NumericalIntegration mMethod = NumericalIntegration.RUNGEKUTTA;


	// Local State for random circuit settings ranges, independent of individual CTRNNs
	public double mBiasMax = 4.0;
	public double mBiasMin = -4.0;
	
	public double mGainMax = 3.0;
	public double mGainMin = 0.0;
	
	public double mTauMax = 3.0;
	public double mTauMin = -0.3;
	
	public double mWeightMax = 10.0;
	public double mWeightMin = -10.0;
	
	public double initialStateMax = 10.0;
	public double initialStateMin = -10.0;
	
	public double externalInputsMax = 10.0;
	public double externalInputsMin = 0.0;


	private static final String[] INLET_ASSIST = new String[]
	{
		"inlet 1 help"
	};
	
	private static final String[] OUTLET_ASSIST = new String[]
	{
		"outlet 1 help"
	};
	
	public CTRNNWrapper(Atom[] args)
	{
		declareInlets(new int[]{DataTypes.ALL});
		declareOutlets(new int[]{DataTypes.ALL});
		
		setInletAssist(INLET_ASSIST);
		setOutletAssist(OUTLET_ASSIST);

		// Input argument 1 is the number of neurons	
		mCircuitSize = args[0].toShort();

		// Uses mCircuitSize to build a new random circuit
	 	// Initializes mSettings & mCTRNN	
		newRandomCircuit();  


		// Inputs, assuming that all neurons are "fully connected" and have inputs.
		// need to implement Hidden neurons
		mInput = new double[mSettings.circuitSize];

		// Timing 
		mClock = new MaxClock(new Callback(this, "clockTick"));	
		mQelem = new MaxQelem(this, "qElemTick");
		
		mTickPeriod = 1.0;  // in milliseconds
	}
    

	/////////////////////////////////////////////////////////
	// Max Messaging 
	/////////////////////////////////////////////////////////

	public void bang()
	{
	}
    
	public void inlet(int i)
	{
	}
    
	public void inlet(float f)
	{
	}

	// This takes the list being sent to this object, as the value of the Input "now"
	public void list(Atom[] list) 
	{
		post("list");
		for(int i = 0; i < mInput.length; i++) 
		{
			if(i < list.length) mInput[i] = list[i].toDouble();
			else mInput[i] = 0.0;

			// post("Input value: " + mInput[0] + " " + mInput[1]);
		}
	}
    

	/////////////////////////////////////////////////////////
	// Timing 
	/////////////////////////////////////////////////////////

	public void start() 
	{ 
		post("start");
		mClock.delay(0.0); 
	} 

	public void stop() 
	{ 
		post("stop");
		mClock.unset(); 
		mQelem.unset();
	} 

	private void clockTick() 
	{     
		mQelem.set();
	}

	private void qElemTick() 
	{ 		
		// Get Inputs
		for(int i = 0; i < mSettings.circuitSize; i++)
			mSettings.setExternalInput(i, mInput[i]);
	

		// Update circuit via numerical integration using
		// either Euler or RungaKutta methods
        switch (mMethod) 
		{
            case RUNGEKUTTA:   mCTRNN.RungaKutta4Step(); break;
            case EULER:		   mCTRNN.EulerStep(); break;
        }


		// Set  Outputs
		double[] output = new double[mSettings.circuitSize];
		for(int i = 0; i < mSettings.circuitSize; i++)
			output[i] = mSettings.getNeuronOutput(i);


		Atom[] outList = new Atom[output.length];
		for(int i = 0; i < output.length; i++) 
		{
			outList[i] = Atom.newAtom(output[i]);
		}
		outlet(0, outList);
		
		// Reschedule to fire again in tickPeriod
		mClock.delay(mTickPeriod);
	} 
	
	public void tickPeriod(double period) 
	{
		// min period in milliseconds is 1
		if(period >= 1.0) mTickPeriod = period;
	}

	public void timeStep(double timeStep) 
	{
		// timeStep is a property
		mCTRNN.setTimeStep(timeStep);
	}


	/////////////////////////////////////////////////////////
	// Misc. 
	/////////////////////////////////////////////////////////
	
	public void notifyDeleted() 
	{
		mClock.release();
		mQelem.release();
	}


	/////////////////////////////////////////////////////////
	// Circuit Evaluation. 
	/////////////////////////////////////////////////////////

	public void transferFunction(int tf) 
	{
		post("transferFunction: "+ tf);

        switch (tf) 
		{
            case 0: mCTRNN.setTransferFunction(CTRNN.TransferFunction.LOGSIG); break;
            case 1: mCTRNN.setTransferFunction(CTRNN.TransferFunction.TANH); break;
            case 2: mCTRNN.setTransferFunction(CTRNN.TransferFunction.TANH_SINE_MIX);break;
            default:mCTRNN.setTransferFunction(CTRNN.TransferFunction.LOGSIG); break; // default for NOW!
        }	
	}

	public void numericalIntegrationMethod(int numIntMethod) 
	{
		post("numericalIntegrationMethod");
        switch (numIntMethod) 
		{
            case 0:  mMethod = NumericalIntegration.RUNGEKUTTA; break;
            case 1:  mMethod = NumericalIntegration.EULER; break;
            default: mMethod = NumericalIntegration.RUNGEKUTTA; break;
        }	
	}


	/////////////////////////////////////////////////////////
	// Circuit Generation. 
	/////////////////////////////////////////////////////////

	public void newRandomCircuit() 
	{
 		// init value with circuit size
	 	// mSettings = new CTRNNSettings(mCircuitSize); 
	 	mSettings = new CTRNNSettings(mCircuitSize, mBiasMax, mBiasMin, mGainMax, mGainMin, mTauMax, mTauMin, mWeightMax, mWeightMin);
		mSettings.RandomizeCircuit();

		// mSettings.RandomizExternalInputs(); // Do we really want to do this?? how about just keep

		mCTRNN = new CTRNN(mSettings);
		mCTRNN.setCenterCrossing();	 // Adjust biases to center crossing values based on connection weights 

		post(" Circuit Settings: " + mSettings.biases[0]);
	}


	/////////////////////////////////////////////////////////
	// Max/min setters  - Used for circuit generation
	/////////////////////////////////////////////////////////

	public void setBiasMax(double biasMax) 
	{
		mBiasMax = biasMax;
		mSettings.mBiasMax = biasMax;
	}
	
	public void setBiasMin(double biasMin) 
	{
		mBiasMin = biasMin;
		mSettings.mBiasMin = biasMin;
	}

	public void setGainMax(double gainMax) 
	{
		mGainMax = gainMax;
		mSettings.mGainMax = gainMax;
	}
	
	public void setGainMin(double gainMin) 
	{
		mGainMin = gainMin;
		mSettings.mGainMin = gainMin;
	}
	
	public void setTauMax(double tauMax) 
	{
		mTauMax = tauMax;
		mSettings.mTauMax = tauMax;
	}
	
	public void setTauMin(double tauMin) 
	{
		mTauMin = tauMin;
		mSettings.mTauMin = tauMin;
	}
	
	public void setWeightMax(double weightMax) 
	{
		mWeightMax = weightMax;
		mSettings.mWeightMax = weightMax;
	}
	
	public void setWeightMin(double weightMin) 
	{
		mWeightMin = weightMin;
		mSettings.mWeightMin = weightMin;
	}


	/////////////////////////////////////////////////////////
	// Getters for (real-time) circuit parameters
	/////////////////////////////////////////////////////////

	public double getNeuronState(int i) 
	{
		return mSettings.getNeuronState(i);
	}
	
	public double getNeuronBias(int i) 
	{
		return mSettings.getNeuronBias(i);
	}
	
	public double getNeuronGain(int i) 
	{
		return mSettings.getNeuronGain(i);
	}
	
	public double getNeuronTimeConstant(int i) 
	{
		return mSettings.getNeuronTimeConstant(i);
	}
	
	public double getExternalInput(int i) 
	{
		return mSettings.getExternalInput(i);
	}
	
	public double getConnectionWeight(int from, int to) 
	{
		return mSettings.getConnectionWeight(from, to);
	}


	/////////////////////////////////////////////////////////
	// Setters for (real-time) circuit parameters
	/////////////////////////////////////////////////////////

	public void setNeuronState(int i, double newState) 
	{
		mSettings.setNeuronState(i, newState);
	}
	
	public void setNeuronBias(int i, double newBias) 
	{
		mSettings.setNeuronBias(i, newBias);
	}
	
	public void setNeuronGain(int i, double newGain) 
	{
		mSettings.setNeuronGain(i, newGain);
	}
	
	public void setNeuronTimeConstant(int i, double newTau) 
	{
		mSettings.setNeuronTimeConstant(i, newTau);
	}
	
	public void setExternalInput(int i, double newExtInput) 
	{
		mSettings.setExternalInput(i, newExtInput);
	}
	
	public void setConnectionWeight(int from, int to, double newWeight) 
	{
		mSettings.setConnectionWeight(from, to, newWeight);
	}
	

	/////////////////////////////////////////////////////////
	// Writing XML & Binary
	/////////////////////////////////////////////////////////

	public void writeCTRNN2XML() 
	{
		try 
		{
			String file = "CTRNN_iroro_outputfile.xml";

			// Create a file output stream.
			FileOutputStream fstream = new FileOutputStream(file);
			
			try 
			{
				// Create XML encoder.
				XMLEncoder ostream = new XMLEncoder(fstream);	
				try 
				{
					ostream.writeObject(mCTRNN);
					ostream.flush();
				} finally {

					ostream.close();
				}
			} finally {
				fstream.close();
			}
		} catch(Exception ex) {
			System.out.println(ex);
		}
	}
	
	public void writeCTRNN2Binary() 
	{
 		try 
		{
			String file = "CTRNN_iroro_outputfile.bin";
			
			// Create file output stream.
			FileOutputStream fstream = new FileOutputStream(file);
			
			try {
				// Create object output stream.
				ObjectOutputStream ostream = new ObjectOutputStream(fstream);
				
				try {
					// Write object.
					ostream.writeObject(mCTRNN);
					ostream.flush();
				} finally {
					ostream.close();
				}
			} finally {
				
				fstream.close();
			}
		} catch(Exception ex) {
			System.out.println(ex);
		}
	}


	/////////////////////////////////////////////////////////
	// Reading XML & Binary
	/////////////////////////////////////////////////////////

	public void readCTRNNXML(String file)
	{
		try 
		{
			// Create file input stream.
			FileInputStream fstream = new FileInputStream(file);
			
			try {
				// Create XML decoder.
				XMLDecoder istream = new XMLDecoder(fstream);
				
				try {
					// Read object.
					Object obj = istream.readObject();
					System.out.println(obj);
				} finally {
					// Close object stream.
					istream.close();
				}
			} finally {
				// Close file stream.
				fstream.close();
			}
		} catch(Exception ex) {
			System.out.println(ex);
		}
	} 
	
	public void readCTRNNBinary(String file) 
	{
		try 
		{
			// Create file input stream.
			FileInputStream fstream = new FileInputStream(file);
			
			try {
				// Create object input stream.
				ObjectInputStream istream = new ObjectInputStream(fstream);
				
				try {
					// Read object.
					Object obj = istream.readObject();
					System.out.println(obj);
				} finally {
					// Close object stream.
					istream.close();
				}
			} finally {
				// Close file stream.
				fstream.close();
			}
		} catch(Exception ex) {
			System.out.println(ex);
		}
	}



}






























