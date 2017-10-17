/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wekaaudioprocessing;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import wekaaudioprocessing.WavFile.WavFile;
import wekaaudioprocessing.WavFile.WavFileException;

/**
 *
 * @author prabhu
 */
public class WekaAudioProcessing {
    private static final long MAX_FRAMES = 300;

    public static void preprocess(String folder, int tag, List<String> out_strings) throws IOException, WavFileException
    {
        File[] files = new File(folder).listFiles();
        int c = 0;

        for (File file : files) {
             if (file.isFile()) {
                 //System.out.println(file);
                 WavFile wavFile = WavFile.openWavFile(file);
                 wavFile.display();
                 int numChannels = wavFile.getNumChannels();
                 long numFrames = wavFile.getNumFrames();
                 double[] buffer = new double[100 * numChannels];
                 int framesRead;
                 int count=0;
                 String out = "";
                 double avg = 0;
                 List<Double> double_list = new ArrayList<Double>();
                 do
                 {
                    framesRead = wavFile.readFrames(buffer, 100);
                   
                    for (int s=0 ; s<framesRead * numChannels ; s++)
                    {
                        avg = avg + buffer[s]*100;
                    }
                    
                    double_list.add(avg/100);
                    count++;
                 }
                 while (framesRead != 0);
                 
                 double max = Collections.max(double_list);
                 
                 out = Double.toString(double_list.get(0)/max);
                 
                 for (int i=1; i<double_list.size();i++)
                 {
                     out = out + "," + Double.toString(double_list.get(i)/max);
                 }
                 
                 for (int i=count; i<MAX_FRAMES; i++){
                     out = out + "," + "0.0";
                 }
                 
                 out = out + "," + tag + "\n";
                 System.out.println(c++);
                 out_strings.add(out);
                 wavFile.close();
             }
        } 
   
    }
    
    public static void data_preprocess(String folder, String evalarff) throws IOException, WavFileException
    {
        String folder_1 = folder+"/1";
        String folder_0 = folder+"/0/";
        BufferedWriter bw = new BufferedWriter(new FileWriter(evalarff));
        
        String out;
        out = "@RELATION audioClassification\n \n";

        for(int i=0; i<MAX_FRAMES; i++){
            out = out + "@ATTRIBUTE attr"+i+" NUMERIC\n";
        }

        out = out + "@ATTRIBUTE class {0,1} \n\n@DATA\n";
        
        bw.write(out);
        List<String> out_strings = new Vector<String>();
        
        preprocess(folder_1, 1, out_strings);
        preprocess(folder_0, 0, out_strings);
        
        Collections.shuffle(out_strings);
        
        for (String string : out_strings){
            bw.write(string);
            //System.out.println(string);
        }
        bw.close();
    }
    
    public static void main(String[] args) throws IOException, WavFileException, Exception
    {
        data_preprocess("dataset_speechtrain/", "dataset_speech/train.arff");
        data_preprocess("dataset_speech/eval/", "dataset_speech/eval.arff");
        DataSource train_source = new DataSource("dataset_speech/train.arff");
        DataSource eval_source = new DataSource("dataset_speech/eval.arff");
        Instances train_data = train_source.getDataSet();
        Instances eval_data = eval_source.getDataSet();
        if (train_data.classIndex() == -1)
            train_data.setClassIndex(train_data.numAttributes() - 1);
        if (eval_data.classIndex() == -1)
            eval_data.setClassIndex(eval_data.numAttributes() - 1);
        
        
        Classifier cls = new LibSVM();
        String options [] = cls.getOptions();
        for (String option:options){
            System.out.print(option+" ");
        }
        System.out.println();
        Evaluation c_eval = new Evaluation(train_data);
        //Cross validation will train and classify
        c_eval.crossValidateModel(cls, train_data, 10, new Random(100));
        System.out.println(c_eval.toSummaryString("\nResults\n======\n", false));
        
        cls.buildClassifier(train_data);
        //Save the model
        weka.core.SerializationHelper.write("dataset_speech/libsvm_speech.model", cls);
        
        //Load the model
        Classifier cls_from_file = (Classifier) weka.core.SerializationHelper.read("dataset_speech/libsvm_speech.model");
        Evaluation eval = new Evaluation(train_data);
        eval.evaluateModel(cls_from_file, eval_data);
        System.out.println(eval.toSummaryString("\nResults\n======\n", false));
        
        //double output = cls_from_file.classifyInstance(eval_data.instance(0));
        //System.out.println(output);
        
    }
}
