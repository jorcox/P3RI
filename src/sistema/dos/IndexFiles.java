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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {
  
  public IndexFiles() {}

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final File docDir = new File(docsPath);
    if (!docDir.exists() || !docDir.canRead()) {
      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(new File(indexPath));
      Analyzer analyzer = new CustomAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(IndexWriter writer, File file)
    throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {

        FileInputStream fis;
        try {
          fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
          // at least on windows, some temporary files raise this exception with an "access denied" message
          // checking if the file can be read doesn't help
          return;
        }

        try {

          // make a new, empty document
          Document doc = new Document();

          // Add the path of the file as a field named "path".  Use a
          // field that is indexed (i.e. searchable), but don't tokenize 
          // the field into separate words and don't index term frequency
          // or positional information:
          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
          doc.add(pathField);

          // Add the last modified date of the file a field named "modified".
          // Use a LongField that is indexed (i.e. efficiently filterable with
          // NumericRangeFilter).  This indexes to milli-second resolution, which
          // is often too fine.  You could instead create a number based on
          // year/month/day/hour/minutes/seconds, down the resolution you require.
          // For example the long value 2011021714 would mean
          // February 17, 2011, 2-3 PM.
          doc.add(new LongField("modified", file.lastModified(), Field.Store.YES));

          DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
          DocumentBuilder parser;
          org.w3c.dom.Document docParseado = null;
          try {			
				parser = dbf.newDocumentBuilder();
				docParseado  = parser.parse(file);            
          } catch (ParserConfigurationException | SAXException e) {
				e.printStackTrace();
          }
          
          
          // Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in UTF-8 encoding.
          // If that's not the case searching for special characters will fail.
          
          
          
          /**
           *  A�ade al indice las etiquetas XML
           */
          String[] listaTags = {"title", "identifier", "subject", "type", "description",
        		  "creator", "publisher", "format", "language"};
          String[] tipos = {"tf", "tf", "tf", "sf", "tf", "tf", "tf", "sf", "sf"};
          
          for (int i=0; i<listaTags.length; i++) {
        	  if (tipos[i].equals("tf")) {
        		  meterTextField(doc, docParseado, listaTags[i]); 
        	  } else if (tipos[i].equals("sf")) {
        		  meterStringField(doc, docParseado, listaTags[i]);
        	  }       	  
          }
          
          /*
           * Indexacion de las coordenadas
           */
          NodeList lista = docParseado.getElementsByTagName("ows:LowerCorner");
          for (int i=0; i<lista.getLength(); i++) {
  			Node nod = lista.item(i);
  			String[] coords = nod.getTextContent().split(" ");
  			//System.out.println("west:" + Double.parseDouble(coords[0]));
  			//System.out.println("south:" + Double.parseDouble(coords[1]));
  			doc.add(new DoubleField("west", Double.parseDouble(coords[0]), Field.Store.YES));
  			doc.add(new DoubleField("south", Double.parseDouble(coords[1]), Field.Store.YES));
  		  }
          lista = docParseado.getElementsByTagName("ows:UpperCorner");
          for (int i=0; i<lista.getLength(); i++) {
  			Node nod = lista.item(i);
  			String[] coords = nod.getTextContent().split(" ");
  			//System.out.println("east:" + Double.parseDouble(coords[0]));
  			//System.out.println("north:" + Double.parseDouble(coords[1]));
  			doc.add(new DoubleField("east", Double.parseDouble(coords[0]), Field.Store.YES));
  			doc.add(new DoubleField("north", Double.parseDouble(coords[1]), Field.Store.YES));
  		  }

          /*
           * Indexacion de las fechas, asumiendo que no hay mas de un issued y un created
           */
          NodeList listaIssued = docParseado.getElementsByTagName("dcterms:issued");
          if (listaIssued.getLength()==1 && listaIssued.item(0).getTextContent()!="") {
        	  String issued = listaIssued.item(0).getTextContent().replace("-","");
              //System.out.println("issued:"+ issued);
              doc.add(new StringField("issued", issued, Field.Store.YES));
          }                    
          
          NodeList listaCreated = docParseado.getElementsByTagName("dcterms:created");
          if (listaCreated.getLength()==1 && listaCreated.item(0).getTextContent()!="") {
        	  String created = listaCreated.item(0).getTextContent().replace("-","");
              //System.out.println("created:"+ created);
              doc.add(new StringField("created", created, Field.Store.YES));
          } 
          
          /*
           * Indexacion de datos temporales (begin y end)
           */
          NodeList listaTemporal = docParseado.getElementsByTagName("dcterms:temporal");
          if (listaTemporal.getLength()==1) {
        	  String temporal = listaTemporal.item(0).getTextContent();
        	  String[] beginEnd = procesarTemporal(temporal);
        	  if (beginEnd!=null) {
        		  //System.out.println("begin:"+beginEnd[0]);
            	  //System.out.println("end:"+beginEnd[1]);
            	  doc.add(new StringField("begin", beginEnd[0], Field.Store.YES));
            	  doc.add(new StringField("end", beginEnd[1], Field.Store.YES));
        	  }       	  
          }

          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("adding " + file);
            writer.addDocument(doc);
          } else {
            // Existing index (an old copy of this document may have been indexed) so 
            // we use updateDocument instead to replace the old one matching the exact 
            // path, if present:
            System.out.println("updating " + file);
            writer.updateDocument(new Term("path", file.getPath()), doc);
          }
          
        } finally {
          fis.close();
        }
      }
    }
  }

	private static void meterTextField(Document doc, org.w3c.dom.Document result, String tag) {
		NodeList lista = result.getElementsByTagName("dc:"+tag);
		for (int i=0; i<lista.getLength(); i++) {
			//System.out.println(tag + " : " + lista.item(i).getTextContent());
			doc.add(new TextField(tag, lista.item(i).getTextContent(), Field.Store.YES));
		}
	}
	
	private static void meterStringField(Document doc, org.w3c.dom.Document result, String tag) {
		NodeList lista = result.getElementsByTagName("dc:"+tag);
		for (int i=0; i<lista.getLength(); i++) {
			//System.out.println(tag + " : " + lista.item(i).getTextContent());
	        doc.add(new StringField(tag, lista.item(i).getTextContent(), Field.Store.YES));
		}
	}
	
	private static String[] procesarTemporal(String texto) {
		String[] res = new String[2];
		int inicioBegin = texto.indexOf("begin=") + "begin=".length();
		int finBegin = texto.indexOf("; ");
		int inicioEnd = texto.indexOf("end=") + "end=".length();
		
		if (inicioBegin>0 && inicioBegin<finBegin && finBegin<inicioEnd) {
			String begin = texto.substring(inicioBegin,finBegin);
			String end = texto.substring(inicioEnd, texto.length()-1);
			res[0] = begin.replace("-", "");
			res[1] = end.replace("-", "");
			return res;
		} else return null;
		
	}
}