/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.server.rooms;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cpsolver.ifs.util.ToolBox;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.model.MapTileCache;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.context.HttpSessionContext;

/**
 * @author Tomas Muller
 */
public class StaticMapServlet extends HttpServlet {
	protected static Log sLog = LogFactory.getLog(StaticMapServlet.class);
	private static final long serialVersionUID = 1L;
	
	private static int sTileSize = 256;
	
	protected SessionContext getSessionContext() {
		return HttpSessionContext.getSessionContext(getServletContext());
	}
	
	protected synchronized BufferedImage fetchTile(int zoom, int x, int y, HttpServletRequest request) throws MalformedURLException, IOException {
		byte[] cached = MapTileCache.get(zoom, x, y);
		if (cached == null) {
			String referer = ApplicationProperty.UniTimeUrl.value();
			if (referer == null)
				referer = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getRequestURI().substring(0, request.getRequestURI().lastIndexOf('/'));
			String agent = request.getHeader("User-Agent");
			String tileURL = ApplicationProperty.RoomUseLeafletMapTiles.value().replace("{s}", String.valueOf((char)('a' + ToolBox.random(3)))).replace("{z}", String.valueOf(zoom)).replace("{x}", String.valueOf(x)).replace("{y}", String.valueOf(y));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = null;
			try {
				URLConnection con = new URL(tileURL).openConnection();
				if (referer != null) con.setRequestProperty("REFERER", referer);
				if (agent != null) con.setRequestProperty("User-Agent", agent);
				in = con.getInputStream();
				byte[] byteChunk = new byte[4096];
				int n;
				while ((n = in.read(byteChunk)) > 0)
					out.write(byteChunk, 0, n);
			} catch (Exception e) {
				sLog.error("Failed to fetch tile " + tileURL + ": " + e.getMessage());
				return null;
			} finally {
				if (in != null) in.close();
			}
			cached = out.toByteArray();
			MapTileCache.put(zoom, x, y, cached);	
		}
		return ImageIO.read(new ByteArrayInputStream(cached));
	}
	
	protected double lonToTile(double lon, int zoom) {
		return ((lon + 180.0) / 360.0) * Math.pow(2.0, zoom);
	}
	
	protected double latToTile(double lat, int zoom) {
		return (1.0 - Math.log(Math.tan(lat * Math.PI / 180.0) + 1.0 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2.0 * Math.pow(2.0, zoom);
	}
	
	protected BufferedImage createBaseMap(int width, int height, double lat, double lon, int zoom, HttpServletRequest request) throws MalformedURLException, IOException {
		double centerX = lonToTile(lon, zoom);
		double centerY = latToTile(lat, zoom);
		
		int startX = (int)Math.floor(centerX - (width / 2.0) / sTileSize);
		int startY = (int)Math.floor(centerY - (height / 2.0) / sTileSize);
		int endX = (int)Math.ceil(centerX + (width / 2.0) / sTileSize);
		int endY = (int)Math.ceil(centerY + (height / 2.0) / sTileSize);
		
		double offsetX = -Math.floor((centerX - Math.floor(centerX)) * sTileSize);
		double offsetY = -Math.floor((centerY - Math.floor(centerY)) * sTileSize);
        offsetX += Math.floor(width / 2.0);
        offsetY += Math.floor(height / 2.0);
        offsetX += Math.floor(startX - Math.floor(centerX)) * sTileSize;
        offsetY += Math.floor(startY - Math.floor(centerY)) * sTileSize;
		
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = result.getGraphics();

        for (int x = startX; x <= endX; x++)
        	for (int y = startY; y <= endY; y++) {
        		BufferedImage tile = fetchTile(zoom, x, y, request);
        		if (tile == null) continue;
        		int destX = (x - startX) * sTileSize + (int)offsetX;
        		int destY = (y - startY) * sTileSize + (int)offsetY;
        		g.drawImage(tile, destX, destY, null);
        	}
        
		BufferedImage shadow = ImageIO.read(StaticMapServlet.class.getClassLoader().getResourceAsStream("org/unitime/timetable/server/resources/marker-shadow.png"));
        g.drawImage(shadow, width / 2 - 12, height / 2 - 41, null);

        BufferedImage marker = ImageIO.read(StaticMapServlet.class.getClassLoader().getResourceAsStream("org/unitime/timetable/server/resources/marker.png"));
        g.drawImage(marker, width / 2 - 12, height / 2 - 41, null);
        
		return result;
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String tile = request.getParameter("tile");
		if (tile != null) {
			String[] params = tile.split(",");
			BufferedImage image = fetchTile(Integer.valueOf(params[0]), Integer.valueOf(params[1]), Integer.valueOf(params[2]), request);
			if (image == null) {
				response.sendError(500, "Failed to fetch a tile, please check the logs for more details.");
				return;
			}
			response.setContentType("image/png");
			response.setDateHeader("Date", System.currentTimeMillis());
			response.setDateHeader("Expires", System.currentTimeMillis() + 604800000l);
			response.setHeader("Cache-control", "public, max-age=604800");
			ImageIO.write(image, "PNG", response.getOutputStream());
			return;
		}
		String center = request.getParameter("center");
		int zoom = Integer.parseInt(request.getParameter("zoom"));
		String size = request.getParameter("size");
		double lat = Double.parseDouble(center.split(",")[0]);
		double lon = Double.parseDouble(center.split(",")[1]);
		int width = Integer.parseInt(size.split("[,x]")[0]);
		int height = Integer.parseInt(size.split("[,x]")[1]);
		BufferedImage image = createBaseMap(width, height, lat, lon, zoom, request);
		response.setContentType("image/png");
		response.setDateHeader("Date", System.currentTimeMillis());
		response.setDateHeader("Expires", System.currentTimeMillis() + 604800000l);
		response.setHeader("Cache-control", "public, max-age=604800");
		ImageIO.write(image, "PNG", response.getOutputStream());
	}

}
