import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

class LoadProperties{
	static String load(String p){
		Properties prop = new Properties();
		try {
			InputStream is = new LoadProperties().getClass().getResourceAsStream("conf.properties");
			prop.load(new BufferedReader(new InputStreamReader(is)));
			is.close();
			return prop.getProperty(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}

public class Process {
	static ArrayList<Centroid> CENTERS = new ArrayList<Centroid>();
	static ArrayList<Point> INSTANCES = new ArrayList<Point>();
	static ArrayList<Centroid> PRE_CENS;
	static int DIMENSION;
	static int MAX_INSTANCE_NUM_NOT_SPLIT = Integer.valueOf(LoadProperties.load("max_instances_num_not_split"));
	static int TRY_TIMES = Integer.valueOf(LoadProperties.load("try_times"));
	//map cluster center results to its evaluation
	static ArrayList<Entry<ArrayList<Centroid>, Double>> RESULTS = new ArrayList<Entry<ArrayList<Centroid>, Double>>(TRY_TIMES);

	static boolean converge(){
		if(PRE_CENS == null)
			return false;
		for(Centroid cc : Process.CENTERS){
			if(!PRE_CENS.contains(cc))
				return false;
		}
		return true;
	}

	//gives your dataset's path and this function will build the internal data structures.
	public static void loadData(String path) throws IOException{
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
		String line;
		line = r.readLine();
		while((line = r.readLine()) != null){
			String[] fs = line.split(",");
			int l=fs.length-1;
			double[] pos = new double[l];
			int i = 0,j = 0;
			for(j=0;j<l;j++){
				pos[i++] = Double.valueOf(fs[j]);
			}
			Process.DIMENSION = l;
			Process.INSTANCES.add(new Point(pos));
		}
		r.close();
	}

	static double evaluate(ArrayList<Centroid> cens){
		double ret = 0.0;
		for(Centroid cc : cens){
			ret += cc.evaluate();
		}
		return ret;
	}

	public static Entry<Integer[], Double> cluster(int k) {
		for(int t = 0; t < Process.TRY_TIMES; t++){
		//random pick the cluster centers
			CENTERS.clear();
			if(PRE_CENS != null)
				PRE_CENS = null;
			Random rand = new Random();
			HashSet<Integer> rSet = new HashSet<Integer>();
			int size = INSTANCES.size();
			while(rSet.size() < k){
				rSet.add(rand.nextInt(size));
			}
			for(int index : rSet){
				Process.CENTERS.add(new Centroid(Process.INSTANCES.get(index)));
			}

			while(!converge()){
				PRE_CENS = new ArrayList<Centroid>(CENTERS);
				ArrayList<Centroid> newCenters = new ArrayList<Centroid>();
				for(Centroid cc : CENTERS){
					cc = cc.getNewCenter();
					newCenters.add(cc);
				}
				CENTERS = newCenters;
			}
			Process.RESULTS.add(new SimpleEntry<ArrayList<Centroid>, Double>(PRE_CENS, Process.evaluate(PRE_CENS)));
		}

		double minEvaluate = Double.MAX_VALUE;
		int minIndex = 0, i = 0;
		for(Entry<ArrayList<Centroid>, Double> entry : RESULTS){
			double e = entry.getValue();
			if(e < minEvaluate){
				minEvaluate = e;
				minIndex = i;
			}
			i++;
		}
		CENTERS = RESULTS.get(minIndex).getKey();
		double evaluate = RESULTS.get(minIndex).getValue();

		Integer[] ret = new Integer[INSTANCES.size()];
		for(int cNum = 0; cNum < CENTERS.size(); cNum++){
			Centroid cc = CENTERS.get(cNum);
			for(int pi : cc.belongedPoints()){
				ret[pi] = cNum;
			}
		}
		return new SimpleEntry<Integer[], Double>(ret, evaluate);
	}

}
