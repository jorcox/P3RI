package sistema.dos;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Simple command-line based search demo. */
public class SearchFiles {

	private SearchFiles() {
	}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		String usage = "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.exit(0);
		}

		String index = "index";
		String field = "contents";
		String queries = null;
		int repeat = 0;
		boolean raw = false;
		String queryString = null;
		int hitsPerPage = 10;
		File entrada = null;
		File salida = null;

		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				index = args[i + 1];
				i++;
			} else if ("-field".equals(args[i])) {
				field = args[i + 1];
				i++;
			} else if ("-queries".equals(args[i])) {
				queries = args[i + 1];
				i++;
			} else if ("-query".equals(args[i])) {
				queryString = args[i + 1];
				i++;
			} else if ("-repeat".equals(args[i])) {
				repeat = Integer.parseInt(args[i + 1]);
				i++;
			} else if ("-raw".equals(args[i])) {
				raw = true;
			} else if ("-paging".equals(args[i])) {
				hitsPerPage = Integer.parseInt(args[i + 1]);
				if (hitsPerPage <= 0) {
					System.err.println("There must be at least 1 hit per page.");
					System.exit(1);
				}
				i++;
			} else if ("-infoNeeds".equals(args[i])) {
				entrada = new File(args[i + 1]);
				i++;
			} else if ("-output".equals(args[i])) {
				salida = new File(args[i + 1]);
				i++;
			}

		}
		/*
		 * Configuración del fichero de salida
		 */
		FileWriter fw = new FileWriter(salida);

		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);

		/*
		 * Configuración de las sotp words
		 
		CharArraySet stopWords = CharArraySet.copy(Version.LUCENE_44, SpanishAnalyzer.getDefaultStopSet());
		Scanner fichero = new Scanner(new FileInputStream(new File("sotpWords.txt")));
		while (fichero.hasNextLine()) {
			stopWords.add(fichero.nextLine());
		}
		Analyzer analyzer = new SpanishAnalyzer(Version.LUCENE_44, stopWords);*/

		MultiFieldQueryParser parser;

		/*
		 * Se analiza el documento XML de entrada para extraer las necesidades
		 * de información que contiene
		 */
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		org.w3c.dom.Document docParseado = null;
		try {
			db = dbf.newDocumentBuilder();
			docParseado = db.parse(entrada);
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}

		/*
		 * Elementos con la etiqueta "text"
		 */
		NodeList textos = docParseado.getElementsByTagName("text");
		NodeList identificadores = docParseado.getElementsByTagName("identifier");

		ArrayList<Necesidad> necesidades = new ArrayList<Necesidad>();

		for (int i = 0; i < textos.getLength(); i++) {
			necesidades.add(new Necesidad(identificadores.item(i).getTextContent(), textos.item(i).getTextContent()));
		}

		/*
		 * Por cada necesidad obtenida se hace un analisis de la misma
		 */
		for (int i = 0; i < necesidades.size(); i++) {

			String line = necesidades.get(i).getTexto();

			String[] listaTags = { "title", "identifier", "subject", "type", "description", "creator", "publisher",
					"format", "language" };
			parser = new MultiFieldQueryParser(listaTags, analyzer);
			Query consulta = parser.parse(line);

			TopDocs results = searcher.search(consulta, 100);
			ScoreDoc[] hits = results.scoreDocs;
			
			for(int j = 0; j < hits.length; j++){    	
		    	  Document doc = searcher.doc(hits[j].doc);
		          String path = doc.get("path");
		          fw.write( (i+1) + "\t" + path.substring(11) + "\n");
		      }
		}
		
		reader.close();
		fw.close();
	}

}