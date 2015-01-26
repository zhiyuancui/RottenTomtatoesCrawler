/**
 * The java crawler for rotten tomatoes
 * The rotten tomatoes offer a API to query its website.
 * This program first get the film name from rotten tomatoes TOP 100 page
 * Then it get the id for this film.
 * In the end, it query the rotten tomatoes by the id.
 * @author Zhiyuan
 *
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.*;

import org.jsoup.safety.Whitelist;  
import org.jsoup.select.Elements;  
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class Crawler {

	private static String apiKey = "Change to your own Key";
	
	/**
	 * This method will return the top 100 movie from the start year
	 * to the end year
	 * @param start the start year
	 * @param end the end year
	 * @return the Movie List in JSON.
	 * @throws Exception: error input start and end
	 */
	public JSONArray getMovieList(int start, int end) throws Exception
	{
		if(start > end ||
			end >2014  ||
			start < 1970)
		{
			throw new Exception();
		}
		
		String url = "http://www.rottentomatoes.com/top/bestofrt/?year=";
		
		
		JSONArray movieListJSONObject= new JSONArray();
		for(int i = start; i<=end;i++)
		{
			
			//retreive the html page by using the url		
			InputStream is = null;
			BufferedReader br;
			String line;
			String HTMLtext="";
			try {
				URL u = new URL(url + i);
				is = u.openStream(); // throws an IOException
				br = new BufferedReader(new InputStreamReader(is));
				while ((line = br.readLine()) != null) {
					HTMLtext += line;
				}
			} 
			catch(Exception e)
			{
				e.printStackTrace();
			}
			ArrayList<String> movieName = parseHTMLPage(HTMLtext);
			//go through the movie list to get all the movie information
			Iterator<String> itm = movieName.iterator();
			
			while(itm.hasNext())
			{
				String name = itm.next();
				JSONObject movieObject = queryRottenTomatoes(name);
				if(movieObject == null)
				{
					continue;
				}
				//System.out.println(movieObject);
				movieListJSONObject.add(movieObject);
				//Write to the file.
				Thread.currentThread().sleep(10000);
			}
			
			//Sleep
			int time = (int)(Math.random()*10000)+10000;
			Thread.currentThread().sleep(time);
		}
		return movieListJSONObject;	
	}
	
	
	/**
	 * This method uses a input html page to get the top 100 movie in same year.
	 * @param HTMLtext
	 * @return the movie name list
	 */
	public ArrayList<String> parseHTMLPage(String HTMLtext)
	{
		ArrayList<String> movieList = new ArrayList<String>();
		Document doc = Jsoup.parse(HTMLtext);
		Element movieTable = doc.select("table.table-striped").first();
		Elements movies = movieTable.select("[target=_top]");
		Iterator<Element> itm = movies.iterator();
		while(itm.hasNext())
		{
			Element e = itm.next();
			movieList.add(e.text());
		}

		return movieList;
	}
	
	public JSONObject queryRottenTomatoes(String movieName)
	{
		System.out.println("Crawler: " + movieName);
		//Crawler the movie information
		movieName = movieName.replaceAll(" ", "%20");
		String URL = "http://api.rottentomatoes.com/api/public/v1.0/movies.json?apikey="
				 + apiKey + "&q="+movieName+"&page_limit=1";
		
		JSONObject movieJSONObject=null; //this is the result after parse the JSON get from rotten tomatoes
		
		try{
			//Get the rotten tomatoes JSON via API
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(URL);
			
			HttpResponse response = client.execute(request);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
								  response.getEntity().getContent()));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			
			//Parse JSON get from the api
			JSONParser jsonParser = new JSONParser();
			Object obj = jsonParser.parse(result.toString());
			JSONObject movieJson = (JSONObject) obj;			
			JSONArray movieInfo = (JSONArray)movieJson.get("movies");
			if(movieInfo == null)
			{
				return null;
			}
			//extract Movie ID and query the movie info using ID
			JSONObject info = (JSONObject)movieInfo.get(0);
			String id = (String)info.get("id");			
			Thread.currentThread().sleep(1000);
			movieJSONObject = queryMovieInfo(id);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return movieJSONObject;
	}
	
	
	/**
	 * This method is used the movie ID to query the rotten tomatoes to
	 * get movie information 
	 * @param ID the movie ID on rotten tomatoes
	 * @return the movie JSON Object
	 */
	public JSONObject queryMovieInfo(String ID)
	{		
		//query the rotten tomatoes via API		
		String URL = "http://api.rottentomatoes.com/api/public/v1.0/movies/"
					+ ID +".json?apikey="+apiKey;
		JSONObject movieInfo=null; //this is the result after parse the JSON get from rotten tomatoes
		JSONObject info = null;
		try{
			//Get the rotten tomatoes JSON via API
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(URL);
			HttpResponse response = client.execute(request);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
								  response.getEntity().getContent()));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			
			//Parse JSON get from the api
			JSONParser jsonParser = new JSONParser();
			Object obj = jsonParser.parse(result.toString());
			movieInfo = (JSONObject) obj;
			//parse the MovieInfo JSON
			info = parseMovieJSON(movieInfo);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return info;
	}
	
	
	/**
	 * The method is used to organize the triples in the json object 
	 * we get from rotten tomatoes.
	 * @param movieInfo the movie json get from rotten tomatoes
	 * @return a movie json just has been organized and cleaned
	 */
	public JSONObject parseMovieJSON(JSONObject movieInfo)
	{
	
		movieInfo.remove("mpaa_rating");
		movieInfo.remove("critics_consensus");
		movieInfo.remove("posters");
		//re-organize releaseDate
		JSONObject releaseDate = (JSONObject)movieInfo.get("release_dates");
		String date = (String)releaseDate.get("theater");
		movieInfo.remove("release_dates");
		movieInfo.put("releaseDate", date);
		
		//re-organize rating
		JSONObject rating = (JSONObject)movieInfo.get("ratings");
		String cr = (String)rating.get("critics_rating");
		movieInfo.put("critics_rating", cr);
		String cs = String.valueOf((long)rating.get("critics_score"));
		movieInfo.put("critics_score", cs);
		String ar = (String)rating.get("audience_rating");
		movieInfo.put("audience_rating", ar);
		String as = String.valueOf((long)rating.get("audience_score"));
		movieInfo.put("audience_score", as);
		
		movieInfo.remove("ratings");
		
		//get review JSON
		JSONObject links = (JSONObject)movieInfo.get("links");
		String url = (String)links.get("reviews");
		movieInfo.remove("links");
		movieInfo.put("reviews",queryReviews(url));
		return movieInfo;
	}
	
	/**
	 * The method uses the reviews url to get the reviews.
	 * @param url: the url for reviews api
	 * @return the review JSON
	 */
	public JSONArray queryReviews(String url)
	{
		try{
			Thread.currentThread().sleep(2000);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		//query the rotten tomatoes via API		
		String URL = url+"?apikey="+apiKey;
		JSONObject reviewInfo=null;
		JSONArray reviews = null;	
		JSONArray newReview = new JSONArray();	
		try{
			//Get the rotten tomatoes JSON via API
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(URL);
			HttpResponse response = client.execute(request);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
								  response.getEntity().getContent()));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			
			//Parse JSON get from the api
			JSONParser jsonParser = new JSONParser();
			Object obj = jsonParser.parse(result.toString());
			reviewInfo = (JSONObject) obj;
			//parse the MovieInfo JSON
			reviews = (JSONArray)reviewInfo.get("reviews");
			//only keep the first 5 reviews
			for(int i=0;i<reviews.size() && i < 5;i++)
			{
				JSONObject r = (JSONObject)reviews.get(i);
				newReview.add(r);
			}			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return reviews;
	}
	
	
	public static void main(String[] args)
	{
		if(args.length != 2)
		{
			System.out.println("Error: wrong argument");
			System.out.println("Usage: startYear endYear");
		}
		Crawler r = new Crawler();
		
		int start = Integer.parseInt(args[0]);
		int end = Integer.parseInt(args[1]);
		
		while(end <= 2014 && start <= end ) {
			String fileName = "rotten";
			fileName = fileName + start + ".txt";
			System.out.println(fileName);
			try{
				//Initial I/O Stream
				BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
				JSONArray movieInfoList = r.getMovieList(start, start);
				Iterator<JSONObject> it = movieInfoList.iterator();
				while(it.hasNext())
				{
					JSONObject movie = it.next();
					out.write(movie.toJSONString());
					out.newLine();
				}
				out.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			start++;
		}
	}
	
}
