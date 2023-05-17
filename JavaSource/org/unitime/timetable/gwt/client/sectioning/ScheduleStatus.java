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
package org.unitime.timetable.gwt.client.sectioning;

import org.unitime.timetable.gwt.client.page.UniTimeNotifications;
import org.unitime.timetable.gwt.client.widgets.P;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.resources.StudentSectioningResources;
import org.unitime.timetable.gwt.shared.PageAccessException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;

/**
 * @author Tomas Muller
 */
public class ScheduleStatus extends P {
	static final StudentSectioningResources RESOURCES =  GWT.create(StudentSectioningResources.class);
	public static final StudentSectioningMessages MESSAGES = GWT.create(StudentSectioningMessages.class);
	public static enum Level {
		INFO("unitime-ScheduleMessage", RESOURCES.statusInfo()),
		WARNING("unitime-ScheduleWarningMessage", RESOURCES.statusWarning()),
		ERROR("unitime-ScheduleErrorMessage", RESOURCES.statusError()),
		DONE("unitime-ScheduleMessage", RESOURCES.statusDone()),
		;
		
		private String iStyleName;
		private ImageResource iIcon;
		
		Level(String styleName, ImageResource icon) {
			iStyleName = styleName;
			iIcon = icon;
		}
		
		public String getStyleName() { return iStyleName; }
		public ImageResource getIcon() { return iIcon; }
	}
	
	private Level iLevel;
	private Image iImage;
	private P iMessage;
	private Command iOnClickCommand;
	
	public ScheduleStatus() {
		super("unitime-ScheduleStatus");
		setVisible(false);
		iImage = new Image(); iImage.setStyleName("image");
		iMessage = new P("message");
		add(iImage); add(iMessage);
		addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (iOnClickCommand != null) iOnClickCommand.execute();
			}
		});
	}
	
	public void setMessage(Level level, String message, Command command) {
		iOnClickCommand = command;
		if (iLevel != null) removeStyleName(iLevel.getStyleName());
		if (message == null || message.isEmpty()) {
			iLevel = null;
			iMessage.setHTML("");
			setVisible(false);
		} else {
			iLevel = level;
			addStyleName(iLevel.getStyleName());
			iMessage.setHTML(message);
			iImage.setResource(iLevel.getIcon());
			iImage.setAltText(message);
			setVisible(true);
		}
		if (iOnClickCommand != null)
			getElement().getStyle().setCursor(Cursor.POINTER);
		else
			getElement().getStyle().clearCursor();
	}
	
	public void setMessage(Level level, String message) {
		setMessage(level, message, null);
	}
	
	public Level getLevel() { return iLevel; }
	public String getMessage() { return iMessage.getHTML(); }
	
	public void info(String message, boolean popup) {
		setMessage(Level.INFO, message);
		if (popup)
			UniTimeNotifications.info(message);
	}
	
	public void info(String message) {
		info(message, true);
	}
	
	public void warning(String message, boolean popup) {
		setMessage(Level.WARNING, message);
		if (popup)
			UniTimeNotifications.warn(message);
	}
	
	public void warning(String message) {
		warning(message, true);
	}
	
	public void error(String message, boolean popup) {
		setMessage(Level.ERROR,  message);
		if (popup)
			UniTimeNotifications.error(message);
	}
	
	public void error(String message, boolean popup, Throwable t) {
		if (t != null && t instanceof PageAccessException && message != null && !message.contains(MESSAGES.sessionExpiredClickToLogin())) {
			setMessage(Level.ERROR, message + "\n" + MESSAGES.sessionExpiredClickToLogin(), new Command() {
				@Override
				public void execute() {
					Window.open("selectPrimaryRole.action?m=" + URL.encodeQueryString(MESSAGES.sessionExpiredClickToLogin())
					+"&target=" + URL.encodeQueryString(Window.Location.getHref()), "_self", "");
				}
			});
		} else {
			setMessage(Level.ERROR, message);
		}
		if (popup)
			UniTimeNotifications.error(message);
	}
	
	public void error(String message) {
		error(message, true);
	}
	
	public void error(String message, Throwable t) {
		if (t != null && t instanceof PageAccessException && message != null && !message.contains(MESSAGES.sessionExpiredClickToLogin())) {
			setMessage(Level.ERROR, message + "\n" + MESSAGES.sessionExpiredClickToLogin(), new Command() {
				@Override
				public void execute() {
					Window.open("selectPrimaryRole.action?m=" + URL.encodeQueryString(MESSAGES.sessionExpiredClickToLogin())
					+"&target=" + URL.encodeQueryString(Window.Location.getHref()), "_self", "");
				}
			});
		} else {
			setMessage(Level.ERROR, message);
		}
		UniTimeNotifications.error(message, t);
	}
	
	public void error(String message, Throwable t, Command callback) {
		setMessage(Level.ERROR,  message, callback);
		UniTimeNotifications.error(message, t, callback);
	}
	
	public void error(Throwable t) {
		error(t.getMessage(), t);
	}
	
	public void done(String message, boolean popup) {
		setMessage(Level.DONE, message);
		if (popup)
			UniTimeNotifications.info(message);
	}
	
	public void done(String message) {
		done(message, true);
	}
	
	public void clear() {
		setMessage(null, null);
	}
	
	public void setOnClickCommand(Command command) {
		iOnClickCommand = command;
	}
}
