import java.io.*;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

public class Start{

	public static void main(String[] args){

		Process P = new Process();
		try{
			String path="Data_with_filename.csv";
			P.loadData(path);
		}
		catch(IOException e){
			System.out.println("Problem in loading the file");
		}

		Entry<Integer[], Double> result = P.cluster(4);

	}

}
