/**
 * Copyright (c) 2015 Carnegie Mellon University.
 * All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS," WITH NO WARRANTIES WHATSOEVER.
 * CARNEGIE MELLON UNIVERSITY EXPRESSLY DISCLAIMS TO THE FULLEST 
 * EXTENT PERMITTEDBY LAW ALL EXPRESS, IMPLIED, AND STATUTORY 
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND 
 * NON-INFRINGEMENT OF PROPRIETARY RIGHTS.

 * This Program is distributed under a BSD license.  
 * Please see license.txt file or permission@sei.cmu.edu for more
 * information. 
 * 
 * DM-0003411
 */

package edu.cmu.emfta.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;

import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.NExpression;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Or;
import com.bpodgursky.jbool_expressions.Variable;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

import edu.cmu.emfta.Event;
import edu.cmu.emfta.Gate;
import edu.cmu.emfta.preferences.PreferencesValues;

/**
 * This class represents the cutset for an FTA
 * The Cutset gathers the list of events required
 * to trigger a fault.
 * 
 * @author julien
 *
 */
public class CutSet {

	class EventComparator implements Comparator<Event> {

		@Override
		public int compare(Event e1, Event e2) {
			return e1.getName().hashCode() - e2.getName().hashCode();
		}
	}

	private List<List<Event>> cutset;

	private Event topEvent;

	/**
	 * Constructor - just pass the FTA as parameter
	 * @param ftaTree
	 */
	public CutSet(Event ftaEvent) {
		System.out.println("[CutSet] constructor");
		cutset = new ArrayList<List<Event>>();
		topEvent = ftaEvent;
	}

	/**
	 * Return the list of cutset. A cutset is a list
	 * of events that will ultimately trigger an occurence
	 * of the main error (top of the FTA).
	 * @return A list of list of event.
	 */
	public List<List<Event>> getCutset() {
		return cutset;
	}

	/**
	 * Output a string of the FTA. Should be used
	 * for debug purpose. If one want to extract
	 * the events, use the getCutset() method.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int i;

		sb.append("Generated Cutset\n");

		i = 0;

		for (List<Event> events : cutset) {
			sb.append("Cutset " + i + ": ");
			for (Event event : events) {
				sb.append(event.getName().toString());
				sb.append(" ");
			}
			sb.append("\n");
			i++;
		}
		return sb.toString();
	}

	/**
	 * Export into a string that represents a CSV file
	 * @return - the string that represents the CSV file
	 */
	public String toCSV() {
		StringBuffer sb = new StringBuffer();
		int i;

		sb.append("Generated Cutset\n");

		i = 0;

		for (List<Event> events : cutset) {
			sb.append("#" + i + ",");
			for (Event event : events) {
				sb.append(event.getName().toString());
				sb.append(",");
			}
			sb.append("\n");
			i++;
		}
		return sb.toString();
	}

	/**
	 * main method that trigger the process
	 * of the FTA (generate everything)
	 */
	public void process() {
		logMessage("[CutSet] processing");
		List<List<Event>> allEvents = processEventUsingJBool(topEvent);
//		List<List<Event>> allEvents = processEvent(topEvent);
		logMessage("[CutSet] cutset size = " + allEvents.size());
		int n = 0;
		for (List<Event> l : allEvents) {
			System.out.print("[CutSet] " + n + ":");
			for (Event e : l) {
				System.out.print(" " + e.getName());

			}
			System.out.print("\n");
			n++;
		}
		cutset = allEvents;
	}

	/**
	 * Used to copy a list of events. Used when
	 * we hit an OR gate - the existing list is 
	 * copied for both branches of the gate
	 * and added to the list of cutset.
	 * @param source
	 * @return
	 */
	public List<Event> copyList(List<Event> source) {
		List<Event> newList;
		newList = new ArrayList<Event>();

		for (Event e : source) {
			newList.add(e);
		}
		return newList;
	}

	/**
	 * Return a list of events corresponding to the gate.
	 * In case of an OR, we will then return a list that
	 * contains the sub-cutset for each event/gate. For
	 * an AND gate, we combined all events and then
	 * combined them to all the sub-cutset from the sub-gates.
	 * 
	 * @param gate - The initial gate of the cutset
	 * @return     - The list of all cutsets
	 */
	public List<List<Event>> processEvent(Event event) {
		List<List<Event>> result;
		result = new ArrayList<List<Event>>();

//		System.out.println("[CutSetAction] calling processGate");
//		System.out.println("[CutSetAction] gate = " + gate);

		if (event.getGate() == null) {
			List<Event> tmp = new ArrayList<Event>();
			tmp.add(event);
			result.add(tmp);
		} else {
			Gate gate = event.getGate();
			switch (gate.getType()) {
			case AND: {
				List<Event> combined;
				combined = new ArrayList<Event>();

				for (Event e : gate.getEvents()) {
					for (Event e2 : gate.getEvents()) {
						if (e == e2) {
							continue;
						}

						for (List<Event> l : processEvent(e)) {

							for (List<Event> l2 : processEvent(e2)) {
								combined = new ArrayList<Event>();

								combined.addAll(l);
								combined.addAll(l2);
								combined.sort(new EventComparator());
								boolean found = false;

								/**
								 * This is done to avoid duplicates.
								 */
								for (List<Event> tmpResult : result) {
									if (tmpResult.containsAll(combined)) {
										found = true;
									}
								}

								if (found == false) {
									result.add(combined);
								}
							}
						}
					}
				}
				break;
			}

			case OR: {
				for (Event e : gate.getEvents()) {
					for (List<Event> l : processEvent(e)) {
						result.add(l);
//						System.out.println("[CutSetAction] OR gate discovered, adding following events");
//						for (Event etmp : l) {
//							System.out.println("[CutSetAction]    -> " + etmp.getName());
//
//						}

					}
				}

				break;
			}

			default: {
				System.err.println("[CutSetAction] default choice not implemented for the generation of the set");
				break;
			}
			}
		}

		if (result.size() == 0) {
			System.out.println("[CutSetAction] result is null");

		}

		//return result;
		
		List<List<Event>> resultnew = new ArrayList<List<Event>>();
		for(List<Event> list : result) {
			List<Event> listnew = list.stream().distinct().collect(Collectors.toList());
			if(! resultnew.contains(listnew)) {
				resultnew.add(listnew);
			}
		}

		return resultnew;
		
	}

	/**
	 * @param event the event to generate the cut set for
	 * 
	 */
	private List<List<Event>> processEventUsingJBool(Event event) {
		Expression<Event> processBoolExpr = processBoolExpr(event);
		logMessage("Topevent: " + processBoolExpr);
		Expression<Event> simplifiedExpr = RuleSet.simplify(processBoolExpr);
		logMessage( "simplified:1: " + simplifiedExpr);
		Expression<Event> dnf = RuleSet.toDNF(processBoolExpr);
		logMessage( "DNF       :3: " + dnf);
		EventComparator eventComparator = new EventComparator();
		
		List<List<Event>> result = new ArrayList<List<Event>>();
		for(Expression<Event> child: ((NExpression<Event>)dnf).expressions){
			List<Event> tempo = new ArrayList<Event>();
			if (child instanceof NExpression) {
				NExpression<Event> nexpr = (NExpression<Event>) child;
				for (Expression<Event> x : nexpr.expressions) {
					if (x instanceof Variable) {
						Variable<Event> var = (Variable<Event>) x;
						tempo.add(var.getValue());
					} else if (x instanceof Not) {
						continue;
					}
				}
			} else if (child instanceof Variable) {
				tempo.add(((Variable<Event>) child).getValue());
			}

			tempo.sort(eventComparator);
			if (!result.contains(tempo)) {
				result.add(tempo);
			}
		}
		
		return result;
	}
	
	/**
	 * @param event
	 * @return
	 */
	public Expression<Event> processBoolExpr(Event event) {
		Expression<Event> result = Variable.of(event);
		logMessage("result: " + result);
		if (event.getGate() != null) {
			Gate gate = event.getGate();
			List<Expression<Event>> interSet = new ArrayList<Expression<Event>>();
			for (Event e : gate.getEvents()) {
				interSet.add(processBoolExpr(e));
				logMessage("interSet: " + interSet);
			}
			switch (gate.getType()) {
			case AND: {
				Expression<Event> interExpr = And.of(interSet);
				logMessage("interExpr: " + interExpr);
				result = result.replaceVars(Collections.singletonMap(event, interExpr));
				logMessage("result: " + result);
				break;
			}

			case OR: {
				Expression<Event> interExpr = Or.of(interSet);
				logMessage("interExpr: " + interExpr);
				result = result.replaceVars(Collections.singletonMap(event, interExpr));
				logMessage("result: " + result);
				break;
			}

			case XOR: {
				if (interSet.size() != 2) {
					System.err.println("[CutSetAction] XOR gate can only have 2 arrguments");
					break;
				}
				Expression<Event> a = interSet.get(0);
				Expression<Event> b = interSet.get(1);
				Expression<Event> interExpr = Or.of(And.of(a, Not.of(b)), And.of(Not.of(a), b));
				result = result.replaceVars(Collections.singletonMap(event, interExpr));
				break;
			}

			default: {
				System.err.println("[CutSetAction] default choice not implemented for the generation of the set");
				break;
			}
			}
		}
		return result;
	}

	private void logMessage(String msg) {
		System.out.println(this.getClass().getName() + ": " + msg);
	}

	public XSSFWorkbook toWorkbook() {
		if (PreferencesValues.useReportMultiPages()) {
			return toMultiSheetsWorkbook();
		} else {
			return toSingleSheetWorkbook();
		}

	}

	public XSSFWorkbook toSingleSheetWorkbook() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		int cutSetIdentifier = 0;
		double cutsetProbability;

		XSSFSheet sheet = workbook.createSheet();
		XSSFRow row;
		XSSFCell cell;
		/* // Have been commented as not required and causes an issue when open with MS Excel.
		 * 
			XSSFTable table = sheet.createTable();
			table.setDisplayName("Cutsets");
			CTTable cttable = table.getCTTable();
	
			// Set which area the table should be placed in
			AreaReference reference = new AreaReference(new CellReference(0, 0), new CellReference(2, 2));
			cttable.setRef(reference.formatAsString());
			cttable.setId((long) 1);
			cttable.setName("Cutsets");
			cttable.setTotalsRowCount((long) 1);
	
			CTTableColumns columns = cttable.addNewTableColumns();
			columns.setCount((long) 3);
			CTTableColumn column;
	
			column = columns.addNewTableColumn();
		 */
		// Create row
		row = sheet.createRow(0);
		CellStyle headingCellStyle = workbook.createCellStyle();
		XSSFFont headingFont = workbook.createFont();
		headingFont.setBold(true);
		headingCellStyle.setFont(headingFont);
		row.setRowStyle(headingCellStyle);

		CellStyle normalCellStyle = workbook.createCellStyle();
		XSSFFont normalFont = workbook.createFont();
		normalFont.setBold(false);
		normalCellStyle.setFont(normalFont);

		for (int j = 0; j < 3; j++) {
			// Create cell
			cell = row.createCell(j);

			switch (j) {
			case 0: {
				cell.setCellValue("Identifier");
				break;
			}
			case 1: {
				cell.setCellValue("Description");
				break;
			}
			case 2: {
				cell.setCellValue("Probability");
				break;
			}

			}
		}

		int rowId = 1;

		for (List<Event> events : cutset) {
			row = sheet.createRow(rowId++);
			row = sheet.createRow(rowId++);
			row.setRowStyle(normalCellStyle);

			cell = row.createCell(0);
			cell.setCellValue("Cutset #" + cutSetIdentifier);

			cutsetProbability = 1;
			for (int i = 0; i < events.size(); i++) {
				cutsetProbability = cutsetProbability * events.get(i).getProbability();
			}

			cell = row.createCell(2);
			if (cutsetProbability != 1) {
				cell.setCellValue("" + cutsetProbability);
			} else {
				cell.setCellValue("" + cutsetProbability);
			}
//			System.out.println("[CutSet] cutset id=" + cutSetIdentifier);

			for (int i = 0; i < events.size(); i++) {
				Event e = events.get(i);

//				System.out.println("[CutSet] event name=" + e.getName());

				// Create row
				row = sheet.createRow(rowId++);
				row.setRowStyle(normalCellStyle);
				for (int j = 0; j < 3; j++) {
					// Create cell
					cell = row.createCell(j);

					switch (j) {
					case 0: {
						cell.setCellValue(e.getName());
						break;
					}
					case 1: {
						cell.setCellValue(e.getDescription());
						break;
					}
					case 2: {
						cell.setCellValue(e.getProbability());
						break;
					}

					}
				}
			}
			cutSetIdentifier = cutSetIdentifier + 1;
		}

		return workbook;

	}

	public XSSFWorkbook toMultiSheetsWorkbook() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		int cutSetIdentifier = 0;
		double cutsetProbability;

		for (List<Event> events : cutset) {

			cutsetProbability = 1;
			for (int i = 0; i < events.size(); i++) {
				cutsetProbability = cutsetProbability * events.get(i).getProbability();
			}

//			System.out.println("[CutSet] cutset id=" + cutSetIdentifier);
			XSSFSheet sheet = workbook.createSheet();

			XSSFTable table = sheet.createTable();
			table.setDisplayName("Cutset");
			CTTable cttable = table.getCTTable();

			// Set which area the table should be placed in
			AreaReference reference = new AreaReference(new CellReference(0, 0), new CellReference(2, 2));
			cttable.setRef(reference.formatAsString());
			cttable.setId((long) 1);
			cttable.setName("Cutset " + cutSetIdentifier);
			cttable.setTotalsRowCount((long) 1);

			CTTableColumns columns = cttable.addNewTableColumns();
			columns.setCount((long) 3);
			CTTableColumn column;
			XSSFRow row;
			XSSFCell cell;

			column = columns.addNewTableColumn();

			// Create row
			row = sheet.createRow(0);
			CellStyle headingCellStyle = workbook.createCellStyle();
			XSSFFont headingFont = workbook.createFont();
			headingFont.setBold(true);
			headingCellStyle.setFont(headingFont);
			row.setRowStyle(headingCellStyle);

			CellStyle normalCellStyle = workbook.createCellStyle();
			XSSFFont normalFont = workbook.createFont();
			normalFont.setBold(false);
			normalCellStyle.setFont(normalFont);

			for (int j = 0; j < 3; j++) {
				// Create cell
				cell = row.createCell(j);

				switch (j) {
				case 0: {
					cell.setCellValue("Identifier");
					break;
				}
				case 1: {
					cell.setCellValue("Description");
					break;
				}
				case 2: {
					if (cutsetProbability == 1) {
						cell.setCellValue("Probability");
					} else {
						cell.setCellValue("Probability (" + cutsetProbability + ")");
					}
					break;
				}

				}
			}

			for (int i = 0; i < events.size(); i++) {
				Event e = events.get(i);

				System.out.println("[CutSet] event name=" + e.getName());
				// Create column
				column = columns.addNewTableColumn();
				column.setName("Column");
				column.setId((long) i + 1);
				// Create row
				row = sheet.createRow(i + 1);
				row.setRowStyle(normalCellStyle);
				for (int j = 0; j < 3; j++) {
					// Create cell
					cell = row.createCell(j);

					switch (j) {
					case 0: {
						cell.setCellValue(e.getName());
						break;
					}
					case 1: {
						cell.setCellValue(e.getDescription());
						break;
					}
					case 2: {
						cell.setCellValue(e.getProbability());
						break;
					}

					}
				}
			}
			cutSetIdentifier = cutSetIdentifier + 1;
		}

		return workbook;

	}
}
