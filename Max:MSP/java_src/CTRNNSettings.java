//
//  CTRNNSettings.java
//  CTRNN
//
//  Created by iroro on 25/06/2010.
//  Copyright (c) 2010-2012 de'fchild productions. All rights reserved.
//


public class CTRNNSettings
{
	public int circuitSize;
	private MersenneTwisterFast prng;	// psuedo-random number generator
	
	// Basic circuit variables
	public double[] biases, gains, outputs, states, taus, invTaus, externalInputs;
	public double[][] weights;
	
	// Ranges for circuit variables, inited to default values
	public double mBiasMax;
	public double mBiasMin;
	
	public double mGainMax;
	public double mGainMin;
	
	public double mTauMax;
	public double mTauMin;
	
	public double mWeightMax;
	public double mWeightMin;
	
	public double initialStateMax;
	public double initialStateMin;
	
	public double externalInputsMax;
	public double externalInputsMin;
	
	public int transferFunction;
	
	// Runga Kutta variables
	public double[] TempStates, TempOutputs, k1, k2, k3, k4;
	
	
	public CTRNNSettings(int size, double biasMax, double biasMin, double gainMax, double gainMin, double tauMax, double tauMin, double weightMax, double weightMin) 
    {
	   circuitSize = size;
	   
	   // Allocate
	   biases = new double[circuitSize];	
	   gains = new double[circuitSize];	
	   outputs = new double[circuitSize];
	   states = new double[circuitSize];
	   taus = new double[circuitSize];	
	   invTaus = new double[circuitSize];
	   
	   externalInputs =  new double[circuitSize];
	   weights = new double[circuitSize][circuitSize]; 
	   
	   // Allocate Runga Kutta variables
	   TempStates = new double[circuitSize];	
	   TempOutputs = new double[circuitSize];
	   
	   k1 = new double[circuitSize];
	   k2 = new double[circuitSize];
	   k3 = new double[circuitSize];	
	   k4 = new double[circuitSize];
		
		// Initialize random circuit "bounds" 
		mBiasMax = biasMax;
		mBiasMin = biasMin;
		
		mGainMax = gainMax;
		mGainMin = gainMin;
		
		mTauMax = tauMax;
		mTauMin = tauMin;
		
		mWeightMax = weightMax;
		mWeightMin = weightMin;
    }

	// random number in the range (min, max) 
	public double getRandomInRange(double min, double max)
	{
		double dist = (max - min);
		double retVal = (prng.nextDouble() * dist) - (dist/2.0);
		return retVal;
	}
	
	public void RandomizeCircuit()
	{
		prng = new MersenneTwisterFast();	// grab a new on every run
		
		for(int i = 0; i < circuitSize; i++)
		{
			biases[i] = getRandomInRange(mBiasMin, mBiasMax);
			gains[i] = getRandomInRange(mGainMin, mGainMax);
			states[i] = getRandomInRange(initialStateMin, initialStateMax);
			taus[i] = getRandomInRange(mTauMin, mTauMax);
			invTaus[i] = 1.0/taus[i];
			
			for(int j = 0; j < circuitSize; j++)
			{
				weights[i][j] = getRandomInRange(mWeightMin, mWeightMax);
			}
		}
		
		// calculate output from states
		updateOutputs();
	}
	
	public void RandomizExternalInputs()
	{
		prng = new MersenneTwisterFast();  // grab a new on every run
		
		for(int i = 0; i < circuitSize; i++)
		{
			externalInputs[i] = getRandomInRange(externalInputsMin, externalInputsMax);
		}
	}
	
	// io havoc: is this necessary?? meaning full for large circuits where dynamics are not fully understood
	void LesionNeuron(int n) 
	{
		for (int i = 0; i < circuitSize; i++) 
		{
			weights[i][n] = 0.0;
			weights[n][i] = 0.0;
		}
	}
	
	public void updateOutputs() 
	{
		for(int i = 0; i < circuitSize; i++)
		{
			outputs[i] = CTRNN.sigmoid(gains[i]*(states[i] + biases[i]));
		}
	}
	
	
	public double getNeuronState(int i) 
	{
		return states[i];
	}
	
	public void setNeuronState(int i, double newState) 
	{
		states[i] = newState;
		outputs[i] = CTRNN.sigmoid(gains[i]*(states[i] + biases[i]));
	}
	
	public double getNeuronOutput(int i) 
	{
		return outputs[i];
	}
	
	//public void setNeuronOutput(int i, double newOutput) 
//	{
//		outputs[i] = newOutput; 
//		states[i] = CTRNN.inverseSigmoid(newOutput)/gains[i] - biases[i];
//	}
	
	public double getNeuronBias(int i) 
	{
		return biases[i];
	}
	
	public void setNeuronBias(int i, double newBias) 
	{
		biases[i] = newBias;
	}
	
	public double getNeuronGain(int i) 
	{
		return gains[i];
	}
	
	public void setNeuronGain(int i, double newGain) 
	{
		gains[i] = newGain;
	}
	
	public double getNeuronTimeConstant(int i) 
	{
		return taus[i];
	}
	
	public void setNeuronTimeConstant(int i, double newTau) 
	{
		taus[i] = newTau;
		invTaus[i] = 1.0/newTau;
	}
	
	public double getExternalInput(int i) 
	{
		return externalInputs[i];
	}
	
	public void setExternalInput(int i, double newExtInput) 
	{
		externalInputs[i] = newExtInput;
	}
	
	public double getConnectionWeight(int from, int to) 
	{
		return weights[from][to];
	}
	
	public void setConnectionWeight(int from, int to, double newWeight) 
	{
		weights[from][to] = newWeight;
	}
	
	

}

