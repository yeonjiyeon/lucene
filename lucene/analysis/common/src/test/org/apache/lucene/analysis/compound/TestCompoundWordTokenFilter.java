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
package org.apache.lucene.analysis.compound;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.compound.hyphenation.HyphenationTree;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.tests.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.tests.analysis.MockTokenizer;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;
import org.xml.sax.InputSource;

public class TestCompoundWordTokenFilter extends BaseTokenStreamTestCase {

  private static CharArraySet makeDictionary(String... dictionary) {
    return new CharArraySet(Arrays.asList(dictionary), true);
  }

  public void testHyphenationCompoundWordsDA() throws Exception {
    CharArraySet dict = makeDictionary("læse", "hest");

    InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
    HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);

    HyphenationCompoundWordTokenFilter tf =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer("min veninde som er lidt af en læsehest"),
            hyphenator,
            dict,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false);
    assertTokenStreamContents(
        tf,
        new String[] {
          "min", "veninde", "som", "er", "lidt", "af", "en", "læsehest", "læse", "hest"
        },
        new int[] {1, 1, 1, 1, 1, 1, 1, 1, 0, 0});
  }

  public void testHyphenationCompoundWordsDELongestMatch() throws Exception {
    CharArraySet dict = makeDictionary("basketball", "basket", "ball", "kurv");

    InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
    HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);

    // the word basket will not be added due to the longest match option
    HyphenationCompoundWordTokenFilter tf =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer("basketballkurv"),
            hyphenator,
            dict,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            40,
            true);
    assertTokenStreamContents(
        tf, new String[] {"basketballkurv", "basketball", "ball", "kurv"}, new int[] {1, 0, 0, 0});
  }

  /**
   * With hyphenation-only, you can get a lot of nonsense tokens. This can be controlled with the
   * min/max subword size.
   */
  public void testHyphenationOnly() throws Exception {
    InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
    HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);

    HyphenationCompoundWordTokenFilter tf =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer("basketballkurv"),
            hyphenator,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            2,
            4);

    // min=2, max=4
    assertTokenStreamContents(
        tf, new String[] {"basketballkurv", "ba", "sket", "ball", "bal", "kurv"});

    tf =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer("basketballkurv"),
            hyphenator,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            4,
            6);

    // min=4, max=6
    assertTokenStreamContents(
        tf, new String[] {"basketballkurv", "basket", "sket", "ball", "lkurv", "kurv"});

    tf =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer("basketballkurv"),
            hyphenator,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            4,
            10);

    // min=4, max=10
    assertTokenStreamContents(
        tf,
        new String[] {
          "basketballkurv",
          "basketball",
          "basketbal",
          "basket",
          "sketball",
          "sketbal",
          "sket",
          "ballkurv",
          "ball",
          "lkurv",
          "kurv"
        });
  }

  public void testDumbCompoundWordsSE() throws Exception {
    CharArraySet dict =
        makeDictionary(
            "Bil", "Dörr", "Motor", "Tak", "Borr", "Slag", "Hammar", "Pelar", "Glas", "Ögon",
            "Fodral", "Bas", "Fiol", "Makare", "Gesäll", "Sko", "Vind", "Rute", "Torkare", "Blad");

    DictionaryCompoundWordTokenFilter tf =
        new DictionaryCompoundWordTokenFilter(
            whitespaceMockTokenizer(
                "Bildörr Bilmotor Biltak Slagborr Hammarborr Pelarborr Glasögonfodral Basfiolsfodral Basfiolsfodralmakaregesäll Skomakare Vindrutetorkare Vindrutetorkarblad abba"),
            dict);

    assertTokenStreamContents(
        tf,
        new String[] {
          "Bildörr",
          "Bil",
          "dörr",
          "Bilmotor",
          "Bil",
          "motor",
          "Biltak",
          "Bil",
          "tak",
          "Slagborr",
          "Slag",
          "borr",
          "Hammarborr",
          "Hammar",
          "borr",
          "Pelarborr",
          "Pelar",
          "borr",
          "Glasögonfodral",
          "Glas",
          "ögon",
          "fodral",
          "Basfiolsfodral",
          "Bas",
          "fiol",
          "fodral",
          "Basfiolsfodralmakaregesäll",
          "Bas",
          "fiol",
          "fodral",
          "makare",
          "gesäll",
          "Skomakare",
          "Sko",
          "makare",
          "Vindrutetorkare",
          "Vind",
          "rute",
          "torkare",
          "Vindrutetorkarblad",
          "Vind",
          "rute",
          "blad",
          "abba"
        },
        new int[] {
          0, 0, 0, 8, 8, 8, 17, 17, 17, 24, 24, 24, 33, 33, 33, 44, 44, 44, 54, 54, 54, 54, 69, 69,
          69, 69, 84, 84, 84, 84, 84, 84, 111, 111, 111, 121, 121, 121, 121, 137, 137, 137, 137, 156
        },
        new int[] {
          7, 7, 7, 16, 16, 16, 23, 23, 23, 32, 32, 32, 43, 43, 43, 53, 53, 53, 68, 68, 68, 68, 83,
          83, 83, 83, 110, 110, 110, 110, 110, 110, 120, 120, 120, 136, 136, 136, 136, 155, 155,
          155, 155, 160
        },
        new int[] {
          1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0,
          0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1
        });
  }

  public void testDumbCompoundWordsSELongestMatch() throws Exception {
    CharArraySet dict =
        makeDictionary(
            "Bil",
            "Dörr",
            "Motor",
            "Tak",
            "Borr",
            "Slag",
            "Hammar",
            "Pelar",
            "Glas",
            "Ögon",
            "Fodral",
            "Bas",
            "Fiols",
            "Makare",
            "Gesäll",
            "Sko",
            "Vind",
            "Rute",
            "Torkare",
            "Blad",
            "Fiolsfodral");

    DictionaryCompoundWordTokenFilter tf =
        new DictionaryCompoundWordTokenFilter(
            whitespaceMockTokenizer("Basfiolsfodralmakaregesäll"),
            dict,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            true);

    assertTokenStreamContents(
        tf,
        new String[] {"Basfiolsfodralmakaregesäll", "Bas", "fiolsfodral", "makare", "gesäll"},
        new int[] {0, 0, 0, 0, 0, 0},
        new int[] {26, 26, 26, 26, 26, 26},
        new int[] {1, 0, 0, 0, 0, 0});
  }

  public void testTokenEndingWithWordComponentOfMinimumLength() throws Exception {
    CharArraySet dict = makeDictionary("ab", "cd", "ef");

    Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    tokenizer.setReader(new StringReader("abcdef"));
    DictionaryCompoundWordTokenFilter tf =
        new DictionaryCompoundWordTokenFilter(
            tokenizer,
            dict,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false);

    assertTokenStreamContents(
        tf,
        new String[] {"abcdef", "ab", "cd", "ef"},
        new int[] {0, 0, 0, 0},
        new int[] {6, 6, 6, 6},
        new int[] {1, 0, 0, 0});
  }

  public void testWordComponentWithLessThanMinimumLength() throws Exception {
    CharArraySet dict = makeDictionary("abc", "d", "efg");

    Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    tokenizer.setReader(new StringReader("abcdefg"));
    DictionaryCompoundWordTokenFilter tf =
        new DictionaryCompoundWordTokenFilter(
            tokenizer,
            dict,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false);

    // since "d" is shorter than the minimum subword size, it should not be added to the token
    // stream
    assertTokenStreamContents(
        tf,
        new String[] {"abcdefg", "abc", "efg"},
        new int[] {0, 0, 0},
        new int[] {7, 7, 7},
        new int[] {1, 0, 0});
  }

  public void testReset() throws Exception {
    CharArraySet dict =
        makeDictionary("Rind", "Fleisch", "Draht", "Schere", "Gesetz", "Aufgabe", "Überwachung");

    MockTokenizer wsTokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    wsTokenizer.setEnableChecks(false); // we will reset in a strange place
    wsTokenizer.setReader(new StringReader("Rindfleischüberwachungsgesetz"));
    DictionaryCompoundWordTokenFilter tf =
        new DictionaryCompoundWordTokenFilter(
            wsTokenizer,
            dict,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false);

    CharTermAttribute termAtt = tf.getAttribute(CharTermAttribute.class);
    tf.reset();
    assertTrue(tf.incrementToken());
    assertEquals("Rindfleischüberwachungsgesetz", termAtt.toString());
    assertTrue(tf.incrementToken());
    assertEquals("Rind", termAtt.toString());
    tf.end();
    tf.close();
    wsTokenizer.setReader(new StringReader("Rindfleischüberwachungsgesetz"));
    tf.reset();
    assertTrue(tf.incrementToken());
    assertEquals("Rindfleischüberwachungsgesetz", termAtt.toString());
  }

  public void testRetainMockAttribute() throws Exception {
    CharArraySet dict = makeDictionary("abc", "d", "efg");
    Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    tokenizer.setReader(new StringReader("abcdefg"));
    TokenStream stream = new MockRetainAttributeFilter(tokenizer);
    stream =
        new DictionaryCompoundWordTokenFilter(
            stream,
            dict,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false);
    MockRetainAttribute retAtt = stream.addAttribute(MockRetainAttribute.class);
    stream.reset();
    while (stream.incrementToken()) {
      assertTrue("Custom attribute value was lost", retAtt.getRetain());
    }
  }

  public void testLucene8124() throws Exception {
    InputSource is =
        new InputSource(getClass().getResource("hyphenation-LUCENE-8124.xml").toExternalForm());
    HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);

    HyphenationCompoundWordTokenFilter tf =
        new HyphenationCompoundWordTokenFilter(whitespaceMockTokenizer("Rindfleisch"), hyphenator);

    assertTokenStreamContents(tf, new String[] {"Rindfleisch", "Rind", "fleisch"});
  }

  public void testNoSubAndNoOverlap() throws Exception { // LUCENE-8183
    String input = "fußballpumpe";
    InputSource is =
        new InputSource(getClass().getResource("hyphenation-LUCENE-8183.xml").toExternalForm());
    HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);
    CharArraySet dictionary = makeDictionary("fuß", "fußball", "ballpumpe", "ball", "pumpe");

    // test the default configuration
    HyphenationCompoundWordTokenFilter tf1 =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input), hyphenator, dictionary);
    assertTokenStreamContents(
        tf1, new String[] {"fußballpumpe", "fußball", "fuß", "ballpumpe", "ball", "pumpe"});

    // test with onlyLongestMatch
    HyphenationCompoundWordTokenFilter tf2 =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            true);
    assertTokenStreamContents(tf2, new String[] {"fußballpumpe", "fußball", "ballpumpe", "pumpe"});

    // test with noSub enabled and noOverlap disabled
    HyphenationCompoundWordTokenFilter tf3 =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            true,
            true,
            false);
    assertTokenStreamContents(tf3, new String[] {"fußballpumpe", "fußball", "ballpumpe"});
    // assert that the onlyLongestMatch state does not matter if noSub is active
    HyphenationCompoundWordTokenFilter tf3b =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false,
            true,
            false);
    assertTokenStreamContents(tf3b, new String[] {"fußballpumpe", "fußball", "ballpumpe"});

    // test with noOverlap enabled
    HyphenationCompoundWordTokenFilter tf4 =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            true,
            true,
            true);
    // NOTE: 'fußball' consumes 'ball' as possible start so 'ballpumpe' is not considered and
    // 'pumpe' is added
    assertTokenStreamContents(tf4, new String[] {"fußballpumpe", "fußball", "pumpe"});

    // assert that the noSub and onlyLongestMatch states do not matter
    HyphenationCompoundWordTokenFilter tf4b =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false,
            false,
            true);
    assertTokenStreamContents(tf4b, new String[] {"fußballpumpe", "fußball", "pumpe"});

    HyphenationCompoundWordTokenFilter tf4c =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            true,
            false,
            true);
    assertTokenStreamContents(tf4c, new String[] {"fußballpumpe", "fußball", "pumpe"});
  }

  public void testNoSubAndTokenInDictionary() throws Exception { // LUCENE-8183
    // test that no subwords are added if the token is part of the dictionary and
    // onlyLongestMatch or noSub is present
    String input = "fußball";
    InputSource is =
        new InputSource(getClass().getResource("hyphenation-LUCENE-8183.xml").toExternalForm());
    HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);
    CharArraySet dictionary = makeDictionary("fußball", "fuß", "ball");

    // test the default configuration as baseline
    HyphenationCompoundWordTokenFilter tf5 =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input), hyphenator, dictionary);
    assertTokenStreamContents(tf5, new String[] {"fußball", "fuß", "ball"});

    // when onlyLongestMatch is enabled fußball matches dictionary. So even so
    // fußball is not added as token it MUST prevent shorter matches to be added
    HyphenationCompoundWordTokenFilter tf6 =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            true,
            false,
            false);
    assertTokenStreamContents(tf6, new String[] {"fußball"});

    // when noSub is enabled fuß and ball MUST NOT be added as subwords as fußball is in the
    // dictionary
    HyphenationCompoundWordTokenFilter tf7 =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false,
            true,
            false);
    assertTokenStreamContents(tf7, new String[] {"fußball"});

    // when noOverlap is enabled fuß and ball MUST NOT be added as subwords as fußball is in the
    // dictionary
    HyphenationCompoundWordTokenFilter tf8 =
        new HyphenationCompoundWordTokenFilter(
            whitespaceMockTokenizer(input),
            hyphenator,
            dictionary,
            CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
            CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
            false,
            false,
            true);
    assertTokenStreamContents(tf8, new String[] {"fußball"});
  }

  public interface MockRetainAttribute extends Attribute {
    void setRetain(boolean attr);

    boolean getRetain();
  }

  public static final class MockRetainAttributeImpl extends AttributeImpl
      implements MockRetainAttribute {
    private boolean retain = false;

    @Override
    public void clear() {
      retain = false;
    }

    @Override
    public boolean getRetain() {
      return retain;
    }

    @Override
    public void setRetain(boolean retain) {
      this.retain = retain;
    }

    @Override
    public void copyTo(AttributeImpl target) {
      MockRetainAttribute t = (MockRetainAttribute) target;
      t.setRetain(retain);
    }

    @Override
    public void reflectWith(AttributeReflector reflector) {
      reflector.reflect(MockRetainAttribute.class, "retain", retain);
    }
  }

  private static class MockRetainAttributeFilter extends TokenFilter {

    MockRetainAttribute retainAtt = addAttribute(MockRetainAttribute.class);

    MockRetainAttributeFilter(TokenStream input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      if (input.incrementToken()) {
        retainAtt.setRetain(true);
        return true;
      } else {
        return false;
      }
    }
  }

  // SOLR-2891
  // *CompoundWordTokenFilter blindly adds term length to offset, but this can take things out of
  // bounds
  // wrt original text if a previous filter increases the length of the word (in this case ü -> ue)
  // so in this case we behave like WDF, and preserve any modified offsets
  public void testInvalidOffsets() throws Exception {
    final CharArraySet dict = makeDictionary("fall");
    final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
    builder.add("ü", "ue");
    final NormalizeCharMap normMap = builder.build();

    Analyzer analyzer =
        new Analyzer() {

          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
            TokenFilter filter = new DictionaryCompoundWordTokenFilter(tokenizer, dict);
            return new TokenStreamComponents(tokenizer, filter);
          }

          @Override
          protected Reader initReader(String fieldName, Reader reader) {
            return new MappingCharFilter(normMap, reader);
          }
        };

    assertAnalyzesTo(
        analyzer,
        "banküberfall",
        new String[] {"bankueberfall", "fall"},
        new int[] {0, 0},
        new int[] {12, 12});
    analyzer.close();
  }

  /** blast some random strings through the analyzer */
  public void testRandomStrings() throws Exception {
    final CharArraySet dict = makeDictionary("a", "e", "i", "o", "u", "y", "bc", "def");
    Analyzer a =
        new Analyzer() {

          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
            return new TokenStreamComponents(
                tokenizer, new DictionaryCompoundWordTokenFilter(tokenizer, dict));
          }
        };
    checkRandomData(random(), a, 200 * RANDOM_MULTIPLIER);
    a.close();

    InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
    final HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);
    Analyzer b =
        new Analyzer() {

          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
            TokenFilter filter = new HyphenationCompoundWordTokenFilter(tokenizer, hyphenator);
            return new TokenStreamComponents(tokenizer, filter);
          }
        };
    checkRandomData(random(), b, 200 * RANDOM_MULTIPLIER);
    b.close();
  }

  public void testEmptyTerm() throws Exception {
    final CharArraySet dict = makeDictionary("a", "e", "i", "o", "u", "y", "bc", "def");
    Analyzer a =
        new Analyzer() {

          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new KeywordTokenizer();
            return new TokenStreamComponents(
                tokenizer, new DictionaryCompoundWordTokenFilter(tokenizer, dict));
          }
        };
    checkOneTerm(a, "", "");
    a.close();

    InputSource is = new InputSource(getClass().getResource("da_UTF8.xml").toExternalForm());
    final HyphenationTree hyphenator = HyphenationCompoundWordTokenFilter.getHyphenationTree(is);
    Analyzer b =
        new Analyzer() {

          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new KeywordTokenizer();
            TokenFilter filter = new HyphenationCompoundWordTokenFilter(tokenizer, hyphenator);
            return new TokenStreamComponents(tokenizer, filter);
          }
        };
    checkOneTerm(b, "", "");
    b.close();
  }

  public void testDecompoundingWithConsumingChars() throws Exception {

    CharArraySet dict = makeDictionary("wein", "schwein", "fleisch");

    Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    String searchTerm = "schweinefleisch";
    DictionaryCompoundWordTokenFilter tf =
        getDictionaryCompoundWordTokenFilter(tokenizer, searchTerm, dict);

    assertTokenStreamContents(tf, new String[] {searchTerm, "schwein", "fleisch"});
  }

  public void testDecompoundingWithConsumingChars2() throws Exception {
    CharArraySet dict = makeDictionary("waffe", "affe", "kampf");

    Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    String searchTerm = "nahkampfwaffen";

    DictionaryCompoundWordTokenFilter tf =
        getDictionaryCompoundWordTokenFilter(tokenizer, searchTerm, dict);

    assertTokenStreamContents(tf, new String[] {searchTerm, "kampf", "waffe"});
  }

  private DictionaryCompoundWordTokenFilter getDictionaryCompoundWordTokenFilter(
      Tokenizer tokenizer, String searchTerm, CharArraySet dict) {
    tokenizer.setReader(new StringReader(searchTerm));
    return new DictionaryCompoundWordTokenFilter(
        tokenizer,
        dict,
        CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
        CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
        CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
        true);
  }
}
