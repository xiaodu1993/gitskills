package simHash;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;


public class Shingle {
	public static final int CHAR_GRAM_LENGTH = 3;

	  public static Set<String> shingles(String line) {

	    HashSet<String> shingles = new HashSet<String>();

	    for (int i = 0; i < line.length() - CHAR_GRAM_LENGTH + 1; i++) {
	      // extract an ngram
	      String shingle = line.substring(i, i + CHAR_GRAM_LENGTH);
	      // get it's index from the dictionary
	      shingles.add(shingle);
	    }
	    return shingles;
	  }

	  public static float jaccard_similarity_coeff(Set<String> shinglesA, Set<String> shinglesB) {
	    float neumerator = CollectionUtils.intersection(shinglesA, shinglesB).size();
	    float denominator = CollectionUtils.union(shinglesA, shinglesB).size();
	    return neumerator / denominator;
	  }
}
