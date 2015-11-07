package simHash;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

public class SimHash {
	public static final int HASH_SIZE = 64;
	public static final long HASH_RANGE = 2 ^ HASH_SIZE;
	public static MurmurHash hasher = new MurmurHash();

	/**
	 * use short cuts to obtains a speed optimized simhash calculation
	 * 
	 * @param s
	 *            input string
	 * @return 64 bit simhash of input string
	 */

	@SuppressWarnings("unused")
	private static final int FIXED_CGRAM_LENGTH = 4;

	public static long computeOptimizedSimHashForString(String s) {
		return computeOptimizedSimHashForString(CharBuffer.wrap(s));
	}

	public static long computeOptimizedSimHashForString(CharBuffer s) {
		Map<String, Integer> tm = segmenterByIK(s.toString());
		LongOpenHashSet shingles = getEveryWordHash(tm, s.length());
		int v[] = new int[HASH_SIZE];
		byte longAsBytes[] = new byte[8];
		for (long shingle : shingles) {
			longAsBytes[0] = (byte) (shingle >> 56);
			longAsBytes[1] = (byte) (shingle >> 48);
			longAsBytes[2] = (byte) (shingle >> 40);
			longAsBytes[3] = (byte) (shingle >> 32);
			longAsBytes[4] = (byte) (shingle >> 24);
			longAsBytes[5] = (byte) (shingle >> 16);
			longAsBytes[6] = (byte) (shingle >> 8);
			longAsBytes[7] = (byte) (shingle);

			long longHash = FPGenerator.std64.fp(longAsBytes, 0, 8);
			for (int i = 0; i < HASH_SIZE; ++i) {
				boolean bitSet = ((longHash >> i) & 1L) == 1L;
				v[i] += (bitSet) ? 1 : -1;
			}
		}

		long simhash = 0;
		for (int i = 0; i < HASH_SIZE; ++i) {
			if (v[i] > 0) {
				simhash |= (1L << i);
			}
		}
		return simhash;
	}

	private static Map<String, Integer> segmenterByIK(String textString) {
		Map<String, Integer> tm = new HashMap<String, Integer>();
		long startTime = System.currentTimeMillis();
		StringReader reader = new StringReader(textString);
		// 当为true时，分词器进行最大词长切分
		IKSegmenter ik = new IKSegmenter(reader, true);
		Lexeme lexeme = null;
		String word = null;
		try {
			while ((lexeme = ik.next()) != null) {
				word = lexeme.getLexemeText();
				if (null != tm.get(word)) {
					tm.put(word, tm.get(word) + 1);
				} else {
					tm.put(word, 1);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("segmenter time : " + (endTime-startTime));
		return tm;
	}

	private static LongOpenHashSet getEveryWordHash(Map<String, Integer> map,
			int length) {
		LongOpenHashSet shingles = new LongOpenHashSet(Math.min(length, 100000));
		Set<String> keys = map.keySet();
		for (Iterator<String> it = keys.iterator(); it.hasNext();) {
			String st = it.next();
			char[] chs = st.toCharArray();
			char temp = '1';
			if (chs.length > 0) {
				temp = chs[0];
			}
			long shingle = temp;
			shingle <<= 16;
			if (chs.length > 1) {
				temp = chs[1];
				shingle |= temp;
				shingle <<= 16;
			}
			if (chs.length > 2) {
				temp = chs[2];
				shingle |= temp;
				shingle <<= 16;
			}
			if (chs.length > 3) {
				temp = chs[3];
				shingle |= temp;
				shingles.add(shingle);
			}
		}
		return shingles;
	}

	public static long computeSimHashFromString(Set<String> shingles) {

		int v[] = new int[HASH_SIZE];
		// compute a set of shingles
		for (String shingle : shingles) {
			byte[] bytes = shingle.getBytes();
			long longHash = FPGenerator.std64.fp(bytes, 0, bytes.length);
			// long hash1 = hasher.hash(bytes, bytes.length, 0);
			// long hash2 = hasher.hash(bytes, bytes.length, (int)hash1);
			// long longHash = (hash1 << 32) | hash2;
			for (int i = 0; i < HASH_SIZE; ++i) {
				boolean bitSet = ((longHash >> i) & 1L) == 1L;
				v[i] += (bitSet) ? 1 : -1;
			}
		}
		long simhash = 0;
		for (int i = 0; i < HASH_SIZE; ++i) {
			if (v[i] > 0) {
				simhash |= (1L << i);
			}
		}

		return simhash;
	}

	public static int hammingDistance(long hash1, long hash2) {
		long bits = hash1 ^ hash2;
		int count = 0;
		while (bits != 0) {
			bits &= bits - 1;
			++count;
		}
		return count;
	}

	public static long rotate(long hashValue) {
		return (hashValue << 1) | (hashValue >>> -1);
	}

	@SuppressWarnings("unused")
	private static String ReadTxtToString(String txtPath) {
		String encoding = "GBK";
		StringBuilder sb = new StringBuilder();
		try {
			InputStreamReader isr = new InputStreamReader(new FileInputStream(
					txtPath), encoding);
			BufferedReader br = new BufferedReader(isr);
			int n = -1;
			while ((n = br.read()) != -1) {
				sb.append(br.readLine());
			}
			if (null != sb) {
				br.close();
			}
			if (null != isr) {
				isr.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void main(String[] args) {

		String string1 = ReadTxtToString("D:/3.txt");
		String string2 = ReadTxtToString("D:/4.txt");
		long timeStart = System.currentTimeMillis();
		long simhash1 = computeSimHashFromString(Shingle.shingles(string1));
		long timeEnd = System.currentTimeMillis();
		System.out.println("Old Calc for Document A Took:"
				+ (timeEnd - timeStart));
		timeStart = System.currentTimeMillis();
		long simhash2 = computeSimHashFromString(Shingle.shingles(string2));
		timeEnd = System.currentTimeMillis();
		System.out.println("Old Calc for Document B Took:"
				+ (timeEnd - timeStart));
		timeStart = System.currentTimeMillis();
		long simhash3 = computeOptimizedSimHashForString(string1);
		timeEnd = System.currentTimeMillis();
		System.out.println("New Calc for Document A Took:"
				+ (timeEnd - timeStart));
		timeStart = System.currentTimeMillis();
		long simhash4 = computeOptimizedSimHashForString(string2);
		timeEnd = System.currentTimeMillis();
		System.out.println("New Calc for Document B Took:"
				+ (timeEnd - timeStart));

		int hammingDistance = hammingDistance(simhash1, simhash2);
		int hammingDistance2 = hammingDistance(simhash3, simhash4);

		System.out.println("the SimHash of doc A: " + simhash1);
		System.out.println("the SimHash of doc A: " + simhash3);
		System.out.println("the SimHash of doc B: " + simhash2);
		System.out.println("the SimHash of doc B: " + simhash4);

		System.out.println("hammingdistance Doc (A) to Doc(B) OldWay:"
				+ hammingDistance);
		System.out.println("hammingdistance Doc (A) to Doc(B) NewWay:"
				+ hammingDistance2);

	}

}