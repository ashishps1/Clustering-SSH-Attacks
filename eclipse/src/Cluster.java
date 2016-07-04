import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
 
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;

import org.jnetpcap.Pcap;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.netlib.util.doubleW;

import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
 
public class Cluster {
 
	static int cluster[][] = new int[3][2];
	static double avgAttr[][] = new double[3][14]; 
	final static int[] arr = new int[14];
	static int instanceCount[] = new int[3];
	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;
		
		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}
 
		return inputReader;
	}
 
	public static void main(String[] args) throws Exception {
		SimpleKMeans kmeans = new SimpleKMeans();
 
		kmeans.setSeed(10);
 
		//important parameter to set: preserver order, number of cluster.
		kmeans.setPreserveInstancesOrder(true);
		
		int numOfClusters = 3;
		
		kmeans.setNumClusters(numOfClusters);
 
		BufferedReader datafile = readDataFile("Data_with_filename.arff");

		String[] options = new String[2];
		options[0] = "-R";                                    // "range"
		options[1] = "15";   		
		Remove remove = new Remove();
		remove.setOptions(options);
		
		Instances data = new Instances(datafile);
		
		String[] pcapFileName = new String[data.numInstances()];
		
		remove.setInputFormat(data);                          // inform filter about dataset **AFTER** setting options
		Instances newData = Filter.useFilter(data, remove);   // apply filter		
		
 
		kmeans.buildClusterer(newData);			//builds the kmeans cluster model.
		
		double squaredSumError =  kmeans.getSquaredError();		//calculates the squared sum error of clusters.
		System.out.println("Squared sum error of clusters is :"+squaredSumError);	
		
		
		PrintWriter writer = new PrintWriter("clusterAssinments.txt", "UTF-8");
		PrintWriter writer1 = new PrintWriter("clusterAnalysis.txt", "UTF-8");

		int[] assignments = kmeans.getAssignments();		//Assigns cluster number to each instance.
		
		String src = "C:\\Users\\ashk43712\\Documents\\Summer Term\\CS F468\\SSH_Sessions\\22\\";
 
		String path = "C:\\Users\\ashk43712\\Documents\\Summer Term\\CS F468\\SSH_Sessions\\22\\Cluster";
		
		path = path.replace("\\", "/");
		
		System.out.println("Deleting existing folders...");
		
		for(int i=0;i<10;i++){
			File f = new File(path+i);
			if (f.exists()){
				String[] entries = f.list();
				for(String s: entries){
				    File currentFile = new File(f.getPath(),s);
				    currentFile.delete();
				}			    
			}			
		}
		
		System.out.println("Creating new folders...");
		
		for(int i=0;i<numOfClusters;i++){
			File f = new File(path+i);			
			new File(path+i).mkdir();
		}
		
		
		cluster[0][0] = 10000;
		cluster[0][1] = -1;
		
		cluster[1][0] = 10000;
		cluster[1][1] = -1;
		
		cluster[2][0] = 10000;
		cluster[2][1] = -1;
		
		int i=0;
		int corruptedFile=0;
		System.out.println("Copying and overriding files ...");
		for(int clusterNum : assignments) {
		    writer.println(String.format("Instance %d -> Cluster %d \n", i, clusterNum));
		    Instance temp = data.get(i);
		    pcapFileName[i] = temp.stringValue(14);
		    //System.out.println(pcapFileName[i]);
		    String source = src;
		    source = source + pcapFileName[i];
		    source = source.replace("\\", "/");
		    
		    String dest = path;
		    dest = dest + clusterNum;
		    dest = dest + "\\"+pcapFileName[i];
		    dest = dest.replace("\\", "/");
		    		    
		    //System.out.println(source);
		    //System.out.println(dest);
		    
		    instanceCount[clusterNum]++;
		    
		    final StringBuilder errbuf = new StringBuilder();  
		    final Pcap pcap = Pcap.openOffline(source, errbuf);
		    
		    
		    arr[0] = 0;
		    
		    try{
			    pcap.loop(Pcap.LOOP_INFINITE, new JPacketHandler<StringBuilder>() {   
		            public void nextPacket(JPacket packet, StringBuilder errbuf) {  
		                  arr[0]++;
		            }
		            
		        }, errbuf);
		    }catch(Exception e){
		    	corruptedFile++;
		    }
		    
		    pcap.close();
		    
		    
		    for(int j=0;j<14;j++){
		    	avgAttr[clusterNum][j]+=arr[j];
		    }

  		    cluster[clusterNum][0] = Math.min(arr[0],cluster[clusterNum][0]);
		    cluster[clusterNum][1] = Math.max(arr[0],cluster[clusterNum][1]);
		    
		    try{
		    	Files.copy(new File(source).toPath(),new File(dest).toPath(),StandardCopyOption.REPLACE_EXISTING);		//copies the files from source to destination.
		    } catch(NoSuchFileException e){
		    	System.err.println("File not found: " + pcapFileName[i]);
		    }
		    
		    i++;
		}
		System.out.println("Files Successfully copied.");
		System.out.println("No. of corrupted files= "+corruptedFile);
		
		System.out.println("Minimum no. of packets in cluster 0 : "+cluster[0][0]);
		System.out.println("Maximum no. of packets in cluster 0 : "+cluster[0][1]);
		System.out.println("\nMinimum no. of packets in cluster 1 : "+cluster[1][0]);
		System.out.println("Maximum no. of packets in cluster 1 : "+cluster[1][1]);
		System.out.println("\nMinimum no. of packets in cluster 2 : "+cluster[2][0]);
		System.out.println("Maximum no. of packets in cluster 2 : "+cluster[2][1]);
		
		int j;
		for(i=0;i<14;i++){
			for(j=0;j<3;j++){
				avgAttr[j][i]=avgAttr[j][i]*1.0/instanceCount[j];
			}
		}
		
		writer.println("Attributes\t\tCluster 0\t\tCluster 1\t\tCluster2");
		writer1.println(String.format("\nAverage Number of packets\t\t%f\t\t%f\t\t%f\n",avgAttr[0][0],avgAttr[1][0],avgAttr[2][0]));
		writer1.println(String.format("\nAverage number of received packets\t\t%f\t\t%f\t\t%f\n",avgAttr[0][1],avgAttr[1][1],avgAttr[2][1]));
		writer1.println(String.format("\nAverage number of sent packets\t\t%f\t\t%f\t\t%f\n",avgAttr[0][2],avgAttr[1][2],avgAttr[2][2]));
		writer1.println(String.format("\n\t\t%f\t\t%f\t\t%f\n",avgAttr[0][3],avgAttr[1][3],avgAttr[2][3]));
		writer1.println(String.format("\n\t\t%f\t\t%f\t\t%f\n",avgAttr[0][4],avgAttr[1][4],avgAttr[2][4]));
		writer1.println(String.format("\n\t\t%f\t\t%f\t\t%f\n",avgAttr[0][5],avgAttr[1][5],avgAttr[2][5]));
		writer1.println(String.format("\n\t\t%f\t\t%f\t\t%f\n",avgAttr[0][6],avgAttr[1][6],avgAttr[2][6]));
		writer1.println(String.format("\n\t\t%f\t\t%f\t\t%f\n",avgAttr[0][7],avgAttr[1][7],avgAttr[2][7]));
		writer1.println(String.format("\n\t\t%f\t\t%f\t\t%f\n",avgAttr[0][8],avgAttr[1][8],avgAttr[2][8]));
		writer1.println(String.format("\n\t\t%f\t\t%f\t\t%f\n",avgAttr[0][9],avgAttr[1][9],avgAttr[2][9]));
		writer1.println(String.format("\n\t\t%f\t\t%f\t\t%f\n",avgAttr[0][10],avgAttr[1][10],avgAttr[2][10]));
		writer1.println(String.format("\nAverage number of packets with ACK flag set\t\t%f\t\t%f\t\t%f\n",avgAttr[0][11],avgAttr[1][11],avgAttr[2][11]));
		writer1.println(String.format("\nAverage number of packets with PSH flag set\t\t%f\t\t%f\t\t%f\n",avgAttr[0][12],avgAttr[1][12],avgAttr[2][12]));
		writer1.println(String.format("\nAverage number of packets with RST flag set\t\t%f\t\t%f\t\t%f\n",avgAttr[0][13],avgAttr[1][13],avgAttr[2][13]));
		
		
		writer.close();
		writer1.close();
	}
}