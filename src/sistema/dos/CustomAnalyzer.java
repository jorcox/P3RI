package sistema.dos;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.es.SpanishLightStemFilter;

public class CustomAnalyzer extends Analyzer{

	@Override
	protected TokenStreamComponents createComponents(String field, Reader reader) {
		Tokenizer source = new LowerCaseTokenizer(reader);
		TokenStream filter = new SpanishLightStemFilter(source);
		return new TokenStreamComponents(source, filter);
	}

}
