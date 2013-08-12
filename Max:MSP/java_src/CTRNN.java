//
//  CTRNN.java
//  CTRNN
//
//  Created by iroro on 25/06/2010.
//  Copyright (c) 2010-2012 de'fchild productions. All rights reserved.
//


public class CTRNN implements java.io.Serializable
{
	public enum TransferFunction { LOGSIG, TANH, TANH_SINE_MIX };
	private static TransferFunction transfunc = TransferFunction.LOGSIG;
	
	private double mTimeStep = 0.01;	// default timestep
	private CTRNNSettings mParams;		// circuit parameters
	private boolean mCenterWeighted = false;	// is center weighted ?
	
	public CTRNN(CTRNNSettings ctrnnSettings) 
	{
		mParams = ctrnnSettings;
	}
	
	
	/////////////////////////////////////////////////////////
	// Transfer Functions (static methods)
	/////////////////////////////////////////////////////////
	
	public static final double mA = 0.7;
	
	public static double sigmoid(double x) 
	{
		return 1.0/(1.0 + Math.exp(-x));
	}
	
	public static double inverseSigmoid(double y)
	{
		return Math.log(y/(1.0 - y));
	}
	
	public static double genericTransferFunction(double y)
	{
		// System.out.println("transfer func: " + transfunc);
		
        switch (transfunc) 
		{
            case LOGSIG:		return sigmoid(y);
            case TANH:			return Math.tanh(y);
            case TANH_SINE_MIX:	return mA * Math.tanh(y) + (1.0 - mA) * Math.sin(y);
			default:			return sigmoid(y);
        }	
	}
	
	
	
	// Adjust biases to their center crossing values 
	// based on the connection weights 
	public void setCenterCrossing()
	{
		double inWeights;
		double thetaS;
		
		for (int i = 0; i < mParams.circuitSize; i++) 
		{
			// Sum the input weights to this neuron
			inWeights = 0;
			
			for (int j = 0; j < mParams.circuitSize; j++)
				inWeights += mParams.getConnectionWeight(j, i);
			
			// Compute the corresponding ThetaStar
			thetaS = -inWeights / 2.0;
			mParams.setNeuronBias(i, thetaS);
		}
		
		mCenterWeighted = true;
	}
	
	
	/////////////////////////////////////////////////////////
	// Numerical Integration
	/////////////////////////////////////////////////////////
	
	// Integrate one step using Euler integration.
	public void EulerStep()
	{
		// System.out.println(" EulerStep ");

		// Update state of all neurons.
		for (int i = 0; i < mParams.circuitSize; i++) 
		{
			double input = mParams.externalInputs[i];
			
			for (int j = 0; j < mParams.circuitSize; j++) 
				input += mParams.weights[j][i] * mParams.outputs[j];
			
			mParams.states[i] += mTimeStep * mParams.invTaus[i] * (input - mParams.states[i]);
		}
		
		// Update outputs of all neurons.
		for (int i = 0; i < mParams.circuitSize; i++)
			mParams.outputs[i] = genericTransferFunction(mParams.gains[i] * (mParams.states[i] + mParams.biases[i]));
	}
	
	// Integrate one step using 4th-order Runge-Kutta
	public void RungaKutta4Step()
	{
		// System.out.println(" RungaKutta4Step ");
		
		double input;
		
		// The first step.
		for (int i = 0; i < mParams.circuitSize; i++) 
		{
			input = mParams.externalInputs[i];
			for (int j = 0; j < mParams.circuitSize; j++)
				input += mParams.weights[j][i] * mParams.outputs[j];
			
			mParams.k1[i] = mTimeStep * mParams.invTaus[i] * (input - mParams.states[i]); 
			mParams.TempStates[i] = mParams.states[i] + 0.5*mParams.k1[i];
			mParams.TempOutputs[i] = genericTransferFunction(mParams.gains[i]*(mParams.TempStates[i]+mParams.biases[i]));
		}
		
		// The second step.
		for (int i = 0; i < mParams.circuitSize; i++) 
		{
			input = mParams.externalInputs[i];
			for (int j = 0; j < mParams.circuitSize; j++)
				input += mParams.weights[j][i] * mParams.TempOutputs[j];
			mParams.k2[i] = mTimeStep * mParams.invTaus[i] * (input - mParams.TempStates[i]);
			mParams.TempStates[i] = mParams.states[i] + 0.5*mParams.k2[i];
		}
		for (int i = 0; i < mParams.circuitSize; i++)
			mParams.TempOutputs[i] = genericTransferFunction(mParams.gains[i]*(mParams.TempStates[i]+mParams.biases[i]));
		
		// The third step.
		for (int i = 0; i <  mParams.circuitSize; i++) 
		{
			input = mParams.externalInputs[i];
			for (int j = 0; j <  mParams.circuitSize; j++)
				input += mParams.weights[j][i] * mParams.TempOutputs[j];
			mParams.k3[i] = mTimeStep * mParams.invTaus[i] * (input - mParams.TempStates[i]);
			mParams.TempStates[i] = mParams.states[i] + mParams.k3[i];
		}
		for (int i = 0; i < mParams.circuitSize; i++)
			mParams.TempOutputs[i] = genericTransferFunction(mParams.gains[i]*(mParams.TempStates[i]+mParams.biases[i]));
		
		// The fourth step.
		for (int i = 0; i < mParams.circuitSize; i++)
		{
			input = mParams.externalInputs[i];
			for (int j = 0; j < mParams.circuitSize; j++)
				input += mParams.weights[j][i] * mParams.TempOutputs[j];
			
			mParams.k4[i] = mTimeStep * mParams.invTaus[i] * (input - mParams.TempStates[i]);
			mParams.states[i] += (1.0/6.0)*mParams.k1[i] + (1.0/3.0)*mParams.k2[i] + (1.0/3.0)*mParams.k3[i] + (1.0/6.0)*mParams.k4[i];
			mParams.outputs[i] = genericTransferFunction(mParams.gains[i]*(mParams.states[i]+mParams.biases[i]));
		}
	}
	
	
	/////////////////////////////////////////////////////////
	// Accessors
	/////////////////////////////////////////////////////////
	
	
	public boolean getIsCenterWeighted() 
	{
		return mCenterWeighted;
	}
	
	public double getTimeStep() 
	{
		return mTimeStep;
	}
	
	public void setTimeStep(double newTimeStep) 
	{
		mTimeStep = newTimeStep;
	}
	
	public void setTransferFunction(TransferFunction tf)
	{
		System.out.println("setTransferFunction " + tf);
		transfunc = tf;
	}	
}


