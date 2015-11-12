package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class EvaluacionSistemas {

	File juicios;
	File resultados;
	File salida;

	public static void main(String[] args) {

		String juiciosP = "";
		String resultadosP = "";
		String salidaP = "";

		for (int i = 0; i < args.length; i++) {
			if ("-qrels".equals(args[i])) {
				juiciosP = args[i + 1];
				i++;
			} else if ("-results".equals(args[i])) {
				resultadosP = args[i + 1];
				i++;
			} else if ("-output".equals(args[i])) {
				salidaP = args[i + 1];
				i++;
			}
		}
		
		try {
			HashMap<String, String> relevancias = mapearJuicios(juiciosP);
			ArrayList<String> resultados = mapearResultados(resultadosP);
			HashMap<String, String> relevancias1 = mapearJuicios(juiciosP, 1);
			ArrayList<String> resultados1 = mapearResultados(resultadosP, 1);
			HashMap<String, String> relevancias2 = mapearJuicios(juiciosP, 2);
			ArrayList<String> resultados2 = mapearResultados(resultadosP, 2);
			
			double[] stats1 = calcularStats(relevancias1, resultados1);
			double[] stats2 = calcularStats(relevancias2, resultados2);
			
			double prec1 = calcularPrec(relevancias1, resultados1, 10);
			double prec2 = calcularPrec(relevancias2, resultados2, 10);
			
			double avgPrec1 = calcularAvgPrec(relevancias1, resultados1);
			double avgPrec2 = calcularAvgPrec(relevancias2, resultados2);
			
			System.out.println("INFORMATION_NEED\t1");
			System.out.println("precision\t" + stats1[0]);
			System.out.println("recall\t" + stats1[1]);
			System.out.println("F1\t" + stats1[2]);
			System.out.println("prec@10\t" + prec1);
			System.out.println("average_precision\t" + avgPrec1);
			System.out.println("recall_precision");
			ArrayList<String> rp1 = calcularRecallPrecision(relevancias1, resultados1);		
			ArrayList<Double> recalls1 = new ArrayList<Double>();
			ArrayList<Double> precisiones1 = new ArrayList<Double>();
			for (String par: rp1) {
				String[] aux1 = par.split(" ");
				recalls1.add(Double.parseDouble(aux1[0]));
				precisiones1.add(Double.parseDouble(aux1[1]));
			}
			for (int i=0; i<recalls1.size(); i++) {
				System.out.println(recalls1.get(i) + "\t" + precisiones1.get(i));
			}
			System.out.println("interpolated_recall_precision");
			double[] x1 = new double[11];
			double[] y1 = new double[11];
			for (int i=0; i<=stats1[1]*10; i++) {
				x1[i] = (double) i / 10;
				y1[i] = getMaxPrecision(recalls1, precisiones1, x1[i]);
				System.out.println(x1[i] + "\t" + y1[i]);
			}
			
			System.out.println("\nINFORMATION_NEED\t2");
			System.out.println("precision\t" + stats2[0]);
			System.out.println("recall\t" + stats2[1]);
			System.out.println("F1\t" + stats2[2]);
			System.out.println("prec@10\t" + prec2);
			System.out.println("average_precision\t" + avgPrec2);
			System.out.println("recall_precision");
			ArrayList<String> rp2 = calcularRecallPrecision(relevancias2, resultados2);
			ArrayList<Double> recalls2 = new ArrayList<Double>();
			ArrayList<Double> precisiones2 = new ArrayList<Double>();
			for (String par: rp2) {
				String[] aux2 = par.split(" ");
				recalls2.add(Double.parseDouble(aux2[0]));
				precisiones2.add(Double.parseDouble(aux2[1]));
			}
			for (int i=0; i<recalls1.size(); i++) {
				System.out.println(recalls1.get(i) + "\t" + precisiones1.get(i));
			}
			System.out.println("interpolated_recall_precision");
			double[] x2 = new double[11];
			double[] y2 = new double[11];
			for (int i=0; i<=stats2[1]*10; i++) {
				x2[i] = (double) i / 10;
				y2[i] = getMaxPrecision(recalls2, precisiones2, x2[i]);
				System.out.println(x2[i] + "\t" + y2[i]);
			}
			
			double precTotal = (stats1[0]+stats2[0])/2;
			double recTotal = (stats1[1]+stats2[1])/2;
			double f1total = (stats1[2]+stats2[2])/2;
			double preca10total = (prec1+prec2)/2;
			double map = (avgPrec1+avgPrec2)/2;
			System.out.println("\n TOTAL");
			System.out.println("precision\t" + precTotal);
			System.out.println("recall\t" + recTotal);
			System.out.println("F1\t" + f1total);
			System.out.println("prec@10\t" + preca10total);
			System.out.println("MAP\t" + map);
			System.out.println("interpolated_recall_precision");
			for (int i=0; i<=(Math.min(stats1[1], stats2[1])*10); i++) {
				System.out.println(x1[i] + "\t" + (y1[i]+y2[i])/2);
			}
		} catch (FileNotFoundException e) {
			System.out.println("Error al leer el fichero");
		}		
	}

	private static double getMaxPrecision(ArrayList<Double> recalls,
			ArrayList<Double> precisiones, double x) {
		double max = 0;
		for (int i=0; i<recalls.size(); i++) {
			if (x <= recalls.get(i) && precisiones.get(i)>max) {
				max = precisiones.get(i);
			}
		}
		return max;
	}

	private static double calcularPrec(HashMap<String, String> relevancias, ArrayList<String> resultados,
			int iter) {
		int V = 0;
		int F = 0;
		for (int i = 0 ; i < iter; i++){		
			String res = relevancias.get(resultados.get(i));
			if(res.equals("0")){
				F++;
			} else {
				V++;
			}
		}
		return V / (double) (V + F);
	}
	
	private static double calcularAvgPrec(HashMap<String, String> relevancias, ArrayList<String> resultados) {
		double acumulado = 0;
		int relevantesRec = 0;
		int recuperados = 0;
		double[] precisiones = new double[resultados.size()];
		
		/* Calcula la precision acumulada cada vez que se recupera un documento relevante */
		for (int i = 0 ; i < resultados.size(); i++){
			recuperados++;
			if (relevancias.get(resultados.get(i)).equals("1")) {
				relevantesRec++;
				precisiones[i] = (double) relevantesRec / recuperados;
			}				
		}
		
		/* Calcula la media de precisiones calculadas */
		for (int i=0; i < precisiones.length; i++) {
			acumulado += precisiones[i];
		}
		return (double) acumulado / relevantesRec;
	}
	
	private static ArrayList<String> calcularRecallPrecision(HashMap<String, String> relevancias, 
			ArrayList<String> resultados) {
		ArrayList<String> res = new ArrayList<String>();
		int totalRelevantes = Collections.frequency(new ArrayList<String>(relevancias.values()), "1");
		int relevantesYRecuperados = 0;
		for (int i = 0 ; i < resultados.size(); i++){
			if (relevancias.get(resultados.get(i)).equals("1")) {
				relevantesYRecuperados++;
				double recall = (double) relevantesYRecuperados / totalRelevantes;			
				double precision = (double) relevantesYRecuperados / (i+1);
				res.add(recall + " " + precision);
			}
		}
		return res;
	}
	
	private static double[] calcularStats(HashMap<String, String> relevancias, ArrayList<String> resultados) {
		int TP = 0;
		int FP = 0;
		int TN = 0;
		int FN = 0;
		double P = 0;
		double R = 0;
		double F1 = 0;
		Iterator it = relevancias.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        String clave = (String) pair.getKey();
	        String valor = (String) pair.getValue();
	        if(resultados.contains(clave) && Integer.parseInt(valor) == 1){
	        	TP++;
	        } else if(resultados.contains(clave) && Integer.parseInt(valor) == 0){
	        	FP++;
	        } else if(!resultados.contains(clave) && Integer.parseInt(valor) == 0){
	        	TN++;
	        } else if(!resultados.contains(clave) && Integer.parseInt(valor) == 1){
	        	FN++;
	        }
	    }
	    P = TP / (double) (TP + FP);
	    R = TP / (double) (TP + FN);
	    F1 = (2*P*R) / (P + R); 
	    return new double[] {P,R,F1};
	}

	private static ArrayList<String> mapearResultados(String resultadosP) throws FileNotFoundException {
		Scanner resultadosR = new Scanner(new File(resultadosP));
		ArrayList<String> arr = new ArrayList<String>();
		while(resultadosR.hasNextLine()){
			String linea = resultadosR.nextLine();
			String[] campos = linea.split("\t");
			arr.add(campos[0] + " " + campos[1]);
		}		
		resultadosR.close();
		return arr;
	}
	
	private static ArrayList<String> mapearResultados(String resultadosP, int necesidad) throws FileNotFoundException {
		Scanner resultadosR = new Scanner(new File(resultadosP));
		ArrayList<String> arr = new ArrayList<String>();
		while(resultadosR.hasNextLine()){
			String linea = resultadosR.nextLine();
			String[] campos = linea.split("\t");
			if (Integer.parseInt(campos[0])==necesidad) {
				arr.add(campos[0] + " " + campos[1]);
			}			
		}		
		resultadosR.close();
		return arr;
	}

	private static HashMap<String, String> mapearJuicios(String juiciosP) throws FileNotFoundException {
		Scanner juiciosR = new Scanner(new File(juiciosP));
		HashMap<String, String> relevancias = new HashMap<String, String>();
		while(juiciosR.hasNextLine()){
			String linea = juiciosR.nextLine();
			String[] campos = linea.split("\t");
			relevancias.put(campos[0] + " " + campos[1], campos[2]);	
		}		
		juiciosR.close();
		return relevancias;
	}
	
	private static HashMap<String, String> mapearJuicios(String juiciosP, int necesidad) throws FileNotFoundException {
		Scanner juiciosR = new Scanner(new File(juiciosP));
		HashMap<String, String> relevancias = new HashMap<String, String>();
		while(juiciosR.hasNextLine()){
			String linea = juiciosR.nextLine();
			String[] campos = linea.split("\t");
			if (Integer.parseInt(campos[0])==necesidad) {
				relevancias.put(campos[0] + " " + campos[1], campos[2]);
			}		
		}		
		juiciosR.close();
		return relevancias;
	}
}
