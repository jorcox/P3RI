package test;


public class EvaluacionSistemas {

	public static void main(String[] args) {

		sistema.uno.IndexFiles.main(new String[] { "-docs", "dublinCore", "-index", "indexUno" });
		sistema.dos.IndexFiles.main(new String[] { "-docs", "dublinCore", "-index", "indexDos" });
		try {
			sistema.uno.SearchFiles
					.main(new String[] { "-infoNeeds", "necesidades.xml", "-output", "sistemaUno.txt", "-index", "indexUno" });
			sistema.dos.SearchFiles
					.main(new String[] { "-infoNeeds", "necesidades.xml", "-output", "sistemaDos.txt", "-index", "indexDos" });
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("------------------------------------------------------------------------\n\n");
		System.out.println("EVALUANDO SISTEMA UNO\n\n");
		System.out.println("------------------------------------------------------------------------\n");
		Evaluacion
				.main(new String[] { "-qrels", "dublinCoreRels.txt", "-results", "sistemaUno.txt", "-output", "testSistemaUno.txt" });
		System.out.println("------------------------------------------------------------------------\n\n");
		System.out.println("EVALUANDO SISTEMA DOS\n\n");
		System.out.println("------------------------------------------------------------------------\n");
		Evaluacion
				.main(new String[] { "-qrels", "dublinCoreRels.txt", "-results", "sistemaDos.txt", "-output", "testSistemaDos.txt" });
	}

}
