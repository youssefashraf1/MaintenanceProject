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
package org.unitime.timetable.gwt.client.aria;

import java.util.ArrayList;
import java.util.List;

import org.unitime.timetable.gwt.resources.GwtAriaMessages;

import com.google.gwt.aria.client.AutocompleteValue;
import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HandlesAllKeyEvents;
import com.google.gwt.event.dom.client.HasBlurHandlers;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

/**
 * @author Tomas Muller
 */
public class AriaSuggestArea extends Composite implements HasText, HasValue<String>, HasSelectionHandlers<Suggestion>, Focusable, HasEnabled, HasAriaLabel {
	private static GwtAriaMessages ARIA = GWT.create(GwtAriaMessages.class);
	private AriaTextArea iText;
	private SuggestOracle iOracle;
	
	private PopupPanel iSuggestionPopup;
	private SuggestionMenu iSuggestionMenu;
	private ScrollPanel iPopupScroll;
	private SuggestionCallback iSuggestionCallback;
	private SuggestOracle.Callback iOracleCallback;
	
	private String iCurrentText = null;
	private boolean iTabPreventDefault = false;
	
	public AriaSuggestArea(SuggestOracle oracle) {
		this(new AriaTextArea(), oracle);
	}
	
	public AriaSuggestArea(AriaTextArea box, List<String> suggestions) {
		this(box, new SimpleOracle(suggestions));
	}
	
	public AriaSuggestArea(AriaTextArea box, SuggestOracle oracle) {
		iOracle = oracle;
		iText = box;
		iText.setStyleName("unitime-TextArea");
		initWidget(iText);
		
		addEventsToTextBox();
		
		iSuggestionMenu = new SuggestionMenu();
		
		iPopupScroll = new ScrollPanel(iSuggestionMenu);
		iPopupScroll.addStyleName("scroll");
		
		iSuggestionPopup = new PopupPanel(true, false);
		iSuggestionPopup.setPreviewingAllNativeEvents(true);
		iSuggestionPopup.setStyleName("unitime-SuggestBoxPopup");
		iSuggestionPopup.setWidget(iPopupScroll);
		iSuggestionPopup.addAutoHidePartner(getElement());
		
		iSuggestionCallback = new SuggestionCallback() {
			@Override
			public void onSuggestionSelected(Suggestion suggestion) {
				if (!suggestion.getReplacementString().isEmpty()) {
					setStatus(ARIA.suggestionSelected(status(suggestion)));
				}
				iCurrentText = suggestion.getReplacementString();
				setText(suggestion.getReplacementString());
				hideSuggestionList();
				fireSuggestionEvent(suggestion);
			}
		};
		
		iOracleCallback = new SuggestOracle.Callback() {
			@Override
			public void onSuggestionsReady(Request request, Response response) {
				if (response.getSuggestions() == null || response.getSuggestions().isEmpty()) {
					if (iSuggestionPopup.isShowing()) iSuggestionPopup.hide();
				} else {
					iSuggestionMenu.clearItems();
					SuggestOracle.Suggestion first = null;
					for (SuggestOracle.Suggestion suggestion: response.getSuggestions()) {
						iSuggestionMenu.addItem(new SuggestionMenuItem(suggestion));
						if (first == null) first = suggestion;
					}
					iSuggestionMenu.selectItem(0);
					iSuggestionMenu.setWidth((iText.getElement().getClientWidth() - 2) + "px");
					iSuggestionPopup.showRelativeTo(iText);
					iSuggestionMenu.scrollToView();
					if (response.getSuggestions().size() == 1) {
						if (first.getReplacementString().isEmpty())
							setStatus(status(first));
						else
							setStatus(ARIA.showingOneSuggestion(status(first)));
					} else {
						setStatus(ARIA.showingMultipleSuggestions(response.getSuggestions().size(), request.getQuery(), status(first)));
					}
				}
			}
		};
		
		Roles.getTextboxRole().setAriaAutocompleteProperty(iText.getElement(), AutocompleteValue.NONE);
		
		iSuggestionPopup.getElement().setAttribute("id", DOM.createUniqueId());
		Roles.getTextboxRole().setAriaOwnsProperty(iText.getElement(), Id.of(iSuggestionPopup.getElement()));
		iText.addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				showSuggestionList();
			}
		});
	}
	
	public void setSuggestions(List<String> suggestions) {
		iOracle = new SimpleOracle(suggestions);
	}
	
	private String status(Suggestion suggestion) {
		return suggestion instanceof HasStatus ? ((HasStatus)suggestion).getStatusString() : suggestion.getDisplayString();
	}
	
	public void setStatus(String text) {
		AriaStatus.getInstance().setText(text);
	}
	
	public void setTabPreventDefault(boolean tabPreventDefault) {
		iTabPreventDefault = tabPreventDefault;
	}
	
	private void addEventsToTextBox() {
		class TextBoxEvents extends HandlesAllKeyEvents implements ValueChangeHandler<String> {
			public void onKeyDown(KeyDownEvent event) {
				switch (event.getNativeKeyCode()) {
				case KeyCodes.KEY_DOWN:
					if (moveSelectionDown()) {
						event.preventDefault();
						return;
					}
					if (!isSuggestionListShowing() && (event.getNativeEvent().getAltKey() || iText.getCursorPos() == iText.getText().length()))
						showSuggestionList();
	            break;
	          case KeyCodes.KEY_UP:
	        	  if (moveSelectionUp()) {
	        		  event.preventDefault();
	        		  return;
	        	  }
	        	  break;
	          case KeyCodes.KEY_ENTER:
	        	  if (isSuggestionListShowing()) {
	        		  iSuggestionMenu.executeSelected();
	        		  event.preventDefault();
	        		  return;
	        	  }
	        	  break;
	          case KeyCodes.KEY_TAB:
	        	  if (isSuggestionListShowing()) {
	        		  iSuggestionMenu.executeSelected();
	        		  if (iTabPreventDefault) {
	        			  event.preventDefault();
	        			  return;
	        		  }
	        	  }
	        	  break;
	          case KeyCodes.KEY_ESCAPE:
	        	  if (isSuggestionListShowing()) {
	        		  hideSuggestionList();
	        		  event.preventDefault();
	        		  return;
	        	  }
				}
				delegateEvent(AriaSuggestArea.this, event);
			}
			
			public void onKeyPress(KeyPressEvent event) {
				delegateEvent(AriaSuggestArea.this, event);
			}
			
			public void onKeyUp(KeyUpEvent event) {
				refreshSuggestions();
				delegateEvent(AriaSuggestArea.this, event);
			}
			
			public void onValueChange(ValueChangeEvent<String> event) {
				delegateEvent(AriaSuggestArea.this, event);
			}
		}
		
		TextBoxEvents events = new TextBoxEvents();
	    events.addKeyHandlersTo(iText);
	    iText.addValueChangeHandler(events);
	}
	
	private boolean moveSelectionDown() {
		if (!isSuggestionListShowing()) return false;
		if (iSuggestionMenu.selectItem(iSuggestionMenu.getSelectedItemIndex() + 1)) {
			if (iSuggestionMenu.getNumItems() > 1)
				setStatus(ARIA.onSuggestion(iSuggestionMenu.getSelectedItemIndex() + 1, iSuggestionMenu.getNumItems(), status(iSuggestionMenu.getSelectedSuggestion())));
			return true;
		} else {
			return false;
		}
	}
	
	private boolean moveSelectionUp() {
		if (!isSuggestionListShowing()) return false;
		boolean selected = false;
		if (iSuggestionMenu.getSelectedItemIndex() == -1) {
			selected = iSuggestionMenu.selectItem(iSuggestionMenu.getNumItems() - 1);
		} else {
			selected = iSuggestionMenu.selectItem(iSuggestionMenu.getSelectedItemIndex() - 1);
		}
		if (selected) {
			if (iSuggestionMenu.getNumItems() > 1)
				setStatus(ARIA.onSuggestion(iSuggestionMenu.getSelectedItemIndex() + 1, iSuggestionMenu.getNumItems(), status(iSuggestionMenu.getSelectedSuggestion())));
			return true;
		} else {
			return false;
		}
	}
	
	public void showSuggestionList() {
		iCurrentText = null;
		refreshSuggestions();
	}
	
	private void refreshSuggestions() {
		String text = getText();
		if (text.equals(iCurrentText)) {
			return;
		} else {
			iCurrentText = text;
		}
		showSuggestions(text);
	}
	
	public void hideSuggestionList() {
		if (iSuggestionPopup.isShowing()) iSuggestionPopup.hide();
	}
	
	public void showSuggestions(String text) {
		iOracle.requestSuggestions(new Request(text), iOracleCallback);
	}
	
	public boolean isSuggestionListShowing() {
		return iSuggestionPopup.isShowing();
	}
	
	public SuggestionMenu getSuggestionMenu() {
		return iSuggestionMenu;
	}

	private class SuggestionMenu extends MenuBar implements HasFocusHandlers, HasBlurHandlers {
		SuggestionMenu() {
			super(true);
			setStyleName("");
			setFocusOnHoverEnabled(false);
			sinkEvents(Event.ONBLUR);
			sinkEvents(Event.ONFOCUS);
		}
		
		@Override
		public void onBrowserEvent(Event event) {
			switch (DOM.eventGetType(event)) {
		    case Event.ONBLUR:
		    	BlurEvent.fireNativeEvent(event, this);
		    	break;
		    case Event.ONFOCUS:
		    	FocusEvent.fireNativeEvent(event, this);
		    	break;
			}
			super.onBrowserEvent(event);
		}
		
		public int getNumItems() {
			return getItems().size();
		}
		
		public int getSelectedItemIndex() {
			MenuItem selectedItem = getSelectedItem();
			if (selectedItem != null)
				return getItems().indexOf(selectedItem);
			return -1;
		}
		
		public boolean selectItem(int index) {
			List<MenuItem> items = getItems();
			if (index > -1 && index < items.size()) {
				selectItem(items.get(index));
				iPopupScroll.ensureVisible(items.get(index));
				return true;
			}
			return false;
		}
		
		public void scrollToView() {
			List<MenuItem> items = getItems();
			int index = getSelectedItemIndex();
			if (index > -1 && index < items.size()) {
				iPopupScroll.ensureVisible(items.get(index));
			}
		}
		
		public boolean executeSelected() {
			MenuItem selected = getSelectedItem();
			if (selected == null) return false;
			selected.getScheduledCommand().execute();
			return true;
		}
		
		public Suggestion getSelectedSuggestion() {
			MenuItem selectedItem = getSelectedItem();
			return selectedItem == null ? null : ((SuggestionMenuItem)selectedItem).getSuggestion();
		}

		@Override
		public HandlerRegistration addBlurHandler(BlurHandler handler) {
			return addHandler(handler, BlurEvent.getType());
		}

		@Override
		public HandlerRegistration addFocusHandler(FocusHandler handler) {
			return addHandler(handler, FocusEvent.getType());
		}
	}
	
	private class SuggestionMenuItem extends MenuItem {
		private Suggestion iSuggestion = null;
		
		private SuggestionMenuItem(final Suggestion suggestion) {
			super(suggestion.getDisplayString(), iOracle.isDisplayStringHTML(), new ScheduledCommand() {
				@Override
				public void execute() {
					iSuggestionCallback.onSuggestionSelected(suggestion);
				}
			});
			setStyleName("item");
			getElement().setAttribute("whiteSpace", "pre-wrap");
			iSuggestion = suggestion;
		}
		
		public Suggestion getSuggestion() {
			return iSuggestion;
		}
	}
	
	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	@Override
	public String getValue() {
		return iText.getValue();
	}

	@Override
	public void setValue(String value) {
		setValue(value, false);
	}

	@Override
	public void setValue(String value, boolean fireEvents) {
		iText.setValue(value);
		if (fireEvents)
			ValueChangeEvent.fire(this, getValue());
	}

	@Override
	public String getText() {
		return iText.getText();
	}

	@Override
	public void setText(String text) {
		iText.setText(text);
	}

	@Override
	public HandlerRegistration addSelectionHandler(SelectionHandler<Suggestion> handler) {
		return addHandler(handler, SelectionEvent.getType());
	}
	
	private void fireSuggestionEvent(Suggestion selectedSuggestion) {
		SelectionEvent.fire(this, selectedSuggestion);
	}

	@Override
	public int getTabIndex() {
		return iText.getTabIndex();
	}

	@Override
	public void setAccessKey(char key) {
		iText.setAccessKey(key);
	}

	@Override
	public void setFocus(boolean focused) {
		iText.setFocus(focused);
	}

	@Override
	public void setTabIndex(int index) {
		iText.setTabIndex(index);
	}
	
	@Override
	public boolean isEnabled() {
		return iText.isEnabled();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		iText.setEnabled(enabled);
	}
	
	public ValueBoxBase<String> getValueBox() {
		return iText;
	}
	
	public static interface SuggestionCallback {
		void onSuggestionSelected(Suggestion suggestion);
	}
	
	public static interface HasStatus {
		public String getStatusString();
	}

	@Override
	public String getAriaLabel() {
		return iText.getAriaLabel();
	}

	@Override
	public void setAriaLabel(String text) {
		iText.setAriaLabel(text);
	}
	
	public static class SimpleOracle extends SuggestOracle {
		List<String> iSuggestions;
		
		public SimpleOracle(List<String> suggestions) {
			iSuggestions = suggestions;
		}

		@Override
		public void requestSuggestions(final Request request, final Callback callback) {
			if (iSuggestions == null || iSuggestions.isEmpty()) {
				callback.onSuggestionsReady(request, new Response(null));
				return;
			}
			List<SimpleSuggestion> suggestions = new ArrayList<SimpleSuggestion>();
			for (String suggestion: iSuggestions) {
				if (suggestion.equalsIgnoreCase(request.getQuery())) continue;
				if (suggestion.toLowerCase().startsWith(request.getQuery().toLowerCase()) || suggestion.toLowerCase().contains(" "+ request.getQuery().toLowerCase()))
					suggestions.add(new SimpleSuggestion(suggestion));
				if (suggestions.size() >= request.getLimit()) break;
			}
			callback.onSuggestionsReady(request, new Response(suggestions));
		}
		
		@Override
		public boolean isDisplayStringHTML() {
			return false;
		}
	}
	
	public static class SimpleSuggestion implements SuggestOracle.Suggestion {
		private String iSuggestion;
		
		SimpleSuggestion(String suggestion) {
			iSuggestion = suggestion;
		}

		@Override
		public String getDisplayString() {
			return iSuggestion;
		}

		@Override
		public String getReplacementString() {
			return iSuggestion;
		}
	}
}