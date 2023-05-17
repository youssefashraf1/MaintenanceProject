package org.unitime.timetable.server.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.command.server.GwtRpcImplements;
import org.unitime.timetable.gwt.resources.GwtConstants;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.EventInterface.FilterRpcResponse;
import org.unitime.timetable.gwt.shared.EventInterface.FilterRpcResponse.Entity;
import org.unitime.timetable.gwt.shared.SuggestionsInterface.DayCode;
import org.unitime.timetable.gwt.shared.SuggestionsInterface.SuggestionsFilterRpcRequest;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.server.FilterBoxBackend;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.service.SolverService;
import org.unitime.timetable.util.Constants;

@GwtRpcImplements(SuggestionsFilterRpcRequest.class)
public class SuggestionsFilterBackend extends FilterBoxBackend<SuggestionsFilterRpcRequest> {
	protected static GwtConstants CONSTANTS = Localization.create(GwtConstants.class);
	protected static GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	@Autowired SolverService<SolverProxy> courseTimetablingSolverService;
	
	@Override
	public FilterRpcResponse execute(SuggestionsFilterRpcRequest request, SessionContext context) {
		context.checkPermission(Right.Suggestions);
		return super.execute(request, context);
	}

	@Override
	public void load(SuggestionsFilterRpcRequest request, FilterRpcResponse response, SessionContext context) {
		if (request.getClassId() != null) {
			SolverProxy solver = courseTimetablingSolverService.getSolver();
			if (solver != null) {
				Map<String, Collection<Entity>> entities = solver.loadSuggestionFilter(request.getClassId());
				if (entities != null)
					for (Map.Entry<String, Collection<Entity>> entry: entities.entrySet())
						response.add(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public static String getDaysName(int days) {
		String ret = "";
		for (DayCode dc: DayCode.values())
			if ((dc.getCode() & days)!=0) ret += CONSTANTS.shortDays()[dc.ordinal()];
		return ret;
	}
	
	public static String slot2time(int timeSinceMidnight, boolean useAmPm) {
		int hour = timeSinceMidnight / 60;
	    int min = timeSinceMidnight % 60;
	    if (useAmPm)
	    	return (hour==0?12:hour>12?hour-12:hour)+":"+(min<10?"0":"")+min+(hour<24 && hour>=12?"p":"a");
	    else
	    	return hour + ":" + (min < 10 ? "0" : "") + min;
	}
	
	public static Map<String, Collection<Entity>> load(Lecture lecture) {
		Map<String, Collection<Entity>> entities = new HashMap<String, Collection<Entity>>();
		if (lecture.getNrRooms() > 0 && lecture.roomLocations() != null) {
			List<Entity> rooms = new ArrayList<Entity>();
			for (RoomLocation r: lecture.roomLocations()) {
				if (r.getPreference() > 500) continue;
				Entity e = new Entity(r.getId(), r.getName(), r.getName());
				e.setCount(r.getRoomSize());
				rooms.add(e);
			}
			entities.put("room", rooms);
		}
		int allDays = 0;
		int firstDay = ApplicationProperty.TimePatternFirstDayOfWeek.intValue();
		if (lecture.timeLocations() != null) {
			Set<Entity> times = new TreeSet<Entity>();
			Set<Entity> dates = new TreeSet<Entity>();
			for (TimeLocation t: lecture.timeLocations()) {
				if (t.getPreference() > 500) continue;
				dates.add(new Entity(t.getDatePatternId(), t.getDatePatternName(), t.getDatePatternName()));
				int days = t.getDayCode();
				if (firstDay != 0) {
					days = t.getDayCode() << firstDay;
					days = (days & 127) + (days >> 7);
				}
				times.add(new Entity(Long.valueOf(- (Constants.sDayCode2Order[days] << 7) - t.getStartSlot()), getDaysName(t.getDayCode()) + " " + t.getStartTimeHeader(CONSTANTS.useAmPm()), getDaysName(t.getDayCode()) + " " + t.getStartTimeHeader(CONSTANTS.useAmPm()) + " - " + t.getEndTimeHeader(CONSTANTS.useAmPm())));
				allDays = (allDays | t.getDayCode());
			}
			entities.put("date", dates);
			entities.put("time", times);
		}
		Set<Entity> daysOfWeek = new TreeSet<Entity>();
		for (int i = 0; i < Constants.DAY_CODES.length; i++) {
			if ((allDays & Constants.DAY_CODES[i]) != 0) {
				Entity e = new Entity(Long.valueOf(i), Constants.DAY_NAMES_FULL[i], CONSTANTS.longDays()[i], "translated-value", CONSTANTS.longDays()[i]);
				e.setProperty("order", String.valueOf((7 + i - firstDay) % 7));
				daysOfWeek.add(e);
			}
		}
		entities.put("day", daysOfWeek);
		List<Entity> flags = new ArrayList<Entity>();
		flags.add(new Entity(1l, "Same Room", MESSAGES.suggestionsSameRoom(), "translated-value", MESSAGES.suggestionsSameRoom()));
		flags.add(new Entity(2l, "Same Time", MESSAGES.suggestionsSameTime(), "translated-value", MESSAGES.suggestionsSameTime()));
		flags.add(new Entity(3l, "Allow Break Hard", MESSAGES.suggestionsAllowBreakHard(), "translated-value", MESSAGES.suggestionsAllowBreakHard()));
		entities.put("flag", flags);
		
		List<Entity> modes = new ArrayList<Entity>();
		modes.add(new Entity(1l, "Suggestions", MESSAGES.suggestionsSuggestions(), "translated-value", MESSAGES.suggestionsSuggestions()));
		modes.add(new Entity(2l, "Placements", MESSAGES.suggestionsPlacements(), "translated-value", MESSAGES.suggestionsPlacements()));
		entities.put("mode", modes);
		
		return entities;
	}

	@Override
	public void suggestions(SuggestionsFilterRpcRequest request, FilterRpcResponse response, SessionContext context) {
	}

	@Override
	public void enumarate(SuggestionsFilterRpcRequest request, FilterRpcResponse response, SessionContext context) {
	}

}
