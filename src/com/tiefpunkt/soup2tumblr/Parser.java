package com.tiefpunkt.soup2tumblr;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.asplode.tumblr.LinkPost;
import net.asplode.tumblr.NoCredentialsException;
import net.asplode.tumblr.PhotoPost;
import net.asplode.tumblr.Post;
import net.asplode.tumblr.QuotePost;
import net.asplode.tumblr.TextPost;
import net.asplode.tumblr.VideoPost;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Parser {
	private final static String TUMBLR_EMAIL = "";
	private final static String TUMBLR_PASSWORD = "";
	private final static String TUMBLR_BLOG = "";
	
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws NoCredentialsException 
	 */
	public static void main(String[] args) throws SAXException, IOException,
			ParserConfigurationException, ParseException, NoCredentialsException {
		
		// Check input parameter
		if (args.length != 1) {
			System.err.println("Y U NO specify input file?!?");
			System.exit(1);
		}

		File f = new File(args[0]);
		if (!f.exists() || !f.canRead()) {
			System.err.println("Y U NO specify real input file?!?");
			System.exit(1);
		}
		
		// Start parsing the soup rss export
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(f);
		doc.getDocumentElement().normalize();
		NodeList items = doc.getElementsByTagName("item");

		JSONParser parser = new JSONParser();
		ContainerFactory containerFactory = new ContainerFactory() {
			public List creatArrayContainer() {
				return new LinkedList();
			}

			public Map createObjectContainer() {
				return new LinkedHashMap();
			}

		};
		
		// Parse the RSS into an Collection of posts
		Collection<Map<String,String>> parsedItems = new LinkedList<Map<String,String>>();
		String jsonText;
		for (int s = 0; s < items.getLength(); s++) {
			Map<String,String> parsedItem = new HashMap<String,String>();
			Node fstNode = items.item(s);

			if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
				Element fstElmnt = (Element) fstNode;
				NodeList fstNmElmntLst = fstElmnt
						.getElementsByTagName("soup:attributes");
				Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
				NodeList fstNm = fstNmElmnt.getChildNodes();
				jsonText = ((Node) fstNm.item(0)).getNodeValue();
				Map json = (Map) parser.parse(jsonText, containerFactory);

				parsedItem.put("type", ""+json.get("type"));
				parsedItem.put("url", ""+json.get("url"));
				parsedItem.put("body", ""+json.get("body"));
				parsedItem.put("source", ""+json.get("source"));
				parsedItem.put("embedcode_or_url", ""+json.get("embedcode_or_url")); 

				fstNmElmntLst = fstElmnt.getElementsByTagName("pubDate");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				fstNm = fstNmElmnt.getChildNodes();
				parsedItem.put("date", ((Node) fstNm.item(0)).getNodeValue());
				
				parsedItems.add(parsedItem);
			}
			//System.out.println(parsedItems.size());
		}
	
		// Create Tumblr Posts out of the soup post collection
		for (Map<String, String> item : parsedItems) {
			//System.out.println(item.get("body"));
			Post p;
			if (item.get("type").equals("video")) {
				VideoPost vp = new VideoPost();
				vp.setEmbedText(item.get("embedcode_or_url"));
				if (item.get("body") != null && !item.get("body").equals("null")) {
					vp.setCaption(convertAllTheSonderzeichen(item.get("body")));
				}
				p = vp;
			} else if (item.get("type").equals("image")) {
				PhotoPost pp = new PhotoPost();
				pp.setSourceURL(item.get("url"));
				if (item.get("source") != null && !item.get("source").equals("null")) {
					pp.setClickThroughURL(item.get("source"));
				}
				if (item.get("body") != null && !item.get("body").equals("null")) {
					pp.setCaption(convertAllTheSonderzeichen(item.get("body")));
				}
				p = pp;
			} else if (item.get("type").equals("regular")) {
				TextPost tp = new TextPost();
				if (item.get("title") != null && !item.get("title").equals("null")) {
					tp.setTitle(convertAllTheSonderzeichen(item.get("title")));
				}
				if (item.get("body") != null && !item.get("body").equals("null")) {
					tp.setBody(convertAllTheSonderzeichen(item.get("body")));
				}
				p = tp;
			} else if (item.get("type").equals("quote")) {
				QuotePost qp = new QuotePost();
				if (item.get("source") != null && !item.get("source").equals("null")) {
					qp.setSource(item.get("source"));
				}
				if (item.get("body") != null && !item.get("body").equals("null")) {
					qp.setQuote(convertAllTheSonderzeichen(item.get("body")));
				}
				p = qp;
			} else if (item.get("type").equals("link")) {
				LinkPost lp = new LinkPost();
				if (item.get("source") != null && !item.get("source").equals("null")) {
					lp.setURL(item.get("source"));
				}
				if (item.get("title") != null && !item.get("title").equals("null")) {
					lp.setName(convertAllTheSonderzeichen(item.get("title")));
				}
				p = lp;
			} else {
				System.out.println(item.get("type"));
				continue;
			}
			
			p.setDate(item.get("date"));
			p.setCredentials(TUMBLR_EMAIL, TUMBLR_PASSWORD);
			p.setBlog(TUMBLR_BLOG);
			p.setGenerator("tiefpunkt's soup2tumblr");
			
			int rc = p.postToTumblr();
			// Wanna-be error-handling....
			if (rc > 220) {
				int x = 1;
				while (rc > 220) {
					System.out.print("!");
					try {
						Thread.sleep(10000*x);
					} catch (InterruptedException e) {
						
					}
					rc = p.postToTumblr();
					x++;
				}
			}
			System.out.print('.');
		}
		
	}
	
	private static String convertAllTheSonderzeichen(String input) {
		input = StringEscapeUtils.escapeHtml4(input);
		input = input.replaceAll("&lt;", "<");
		input = input.replaceAll("&gt;", ">");
		input = input.replaceAll("=&quot;", "=\"");
		input = input.replaceAll("&quot;>", "\">");
		return input;
		
	}
}
