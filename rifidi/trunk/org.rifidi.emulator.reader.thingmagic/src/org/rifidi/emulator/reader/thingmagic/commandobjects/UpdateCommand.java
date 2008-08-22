package org.rifidi.emulator.reader.thingmagic.commandobjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rifidi.emulator.reader.thingmagic.commandobjects.exceptions.CommandCreationExeption;
import org.rifidi.emulator.reader.thingmagic.database.IDBRow;
import org.rifidi.emulator.reader.thingmagic.database.IDBTable;
import org.rifidi.emulator.reader.thingmagic.module.ThingMagicReaderSharedResources;

public class UpdateCommand implements Command {
	private static Log logger = LogFactory.getLog(UpdateCommand.class);

	private String command;
	private String table;

	Map<String, String> keyValuePairs = new HashMap<String, String>();

	private ThingMagicReaderSharedResources tmsr;

	public UpdateCommand(String command, ThingMagicReaderSharedResources tmsr)
			throws CommandCreationExeption {
		// TODO Auto-generated constructor stub
		this.command = command;
		this.tmsr = tmsr;

		List<String> tokens = new ArrayList<String>();
		/*
		 * This regex looks for a Word, or a series of spaces on either side of
		 * any single comparison operator or comma, or a single parentheses
		 * (opening or closing). At the last ... any dangling spaces not
		 * attached to the above groups and then anything else as a single
		 * group.
		 * 
		 * This makes it really easy to parse the command string as it becomes
		 * really predictable command blocks.
		 */
		Pattern tokenizer = Pattern.compile(
				"\\w+|\\s*<>\\*|\\s*>=\\s*|\\s*<=\\s*|\\s*=\\s*|\\s*,\\s*|\\s?+|"
						+ ">|<|\\(|\\)|'|[^\\s\\w,<>=\\(\\)']+",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher tokenFinder = tokenizer.matcher(command.toLowerCase().trim());

		while (tokenFinder.find()) {
			String temp = tokenFinder.group();
			/*
			 * no need to add empty strings at tokens.
			 */
			// TODO: Figure out why we are getting empty stings as tokens.
			if (temp.equals(""))
				continue;
			tokens.add(temp);
		}

		logger.debug(tokens);

		ListIterator<String> tokenIterator = tokens.listIterator();
		String token = tokenIterator.next();

		if (!token.equals("update"))
			throw new CommandCreationExeption(
					"Error 0100:     syntax error at '" + token + "'");

		try {
			token = tokenIterator.next();

			if (!token.matches("\\s+"))
				throw new CommandCreationExeption(
						"Error 0100:     syntax error at '" + token + "'");

			token = tokenIterator.next();

			table = token;
			if (!table.matches("\\w+"))
				throw new CommandCreationExeption(
						"Error 0100:     syntax error at '" + table + "'");

			token = tokenIterator.next();

			if (!token.matches("\\s+"))
				throw new CommandCreationExeption(
						"Error 0100:     syntax error at '" + token + "'");

			token = tokenIterator.next();

			if (!token.equals("set"))
				throw new CommandCreationExeption(
						"Error 0100:     syntax error at '" + token + "'");

			token = tokenIterator.next();

			parseKeyValuePairs(tokenIterator);
		} catch (NoSuchElementException e) {
			/*
			 * if we get here... we run out of tokens prematurely... Our job now is
			 * to walk backwards to find the last non space tokens and
			 * throw an exception saying that there is an syntax error at that
			 * point.
			 */

			/*
			 * look for the last offending command block that is not a series of
			 * whitespaces.
			 */

			token = tokenIterator.previous();
			while (token.matches("\\s")) {
				token = tokenIterator.previous();
			}

			throw new CommandCreationExeption(
					"Error 0100:     syntax error at '" + token + "'");

		}

		IDBTable tableImpl = tmsr.getDataBase().getTable(table);
		if (tableImpl == null) {
			throw new CommandCreationExeption(
					"Error 0100:     syntax error at '" + table + "'");
		}

		for (int x = 0; x < tableImpl.size(); x++) {
			IDBRow row = tableImpl.get(x);
			for (String column : keyValuePairs.keySet()) {
				if (!row.containsColumn(column)) {
					throw new CommandCreationExeption(
							"Error 0100:     Unknown " + column);
				}

				// if (!row.isReadable(column)) {
				// throw new CommandCreationExeption(
				// "Error 0100:     Could not read from '" + column
				// + "' in '" + table + "'");
				// }
			}
		}
	}

	/*
	 * private helper method to help break up the logical layout of the parser.
	 */
	private void parseKeyValuePairs(ListIterator<String> tokenIterator)
			throws CommandCreationExeption, NoSuchElementException {
		// TODO Auto-generated method stub
		String token;
		do {
			token = tokenIterator.next();

			if (!token.matches("\\w+"))
				throw new CommandCreationExeption(
						"Error 0100:     syntax error at '" + token + "'");

			String key = token;

			token = tokenIterator.next();

			if (!token.matches("\\s*=\\s*"))
				throw new CommandCreationExeption(
						"Error 0100:     syntax error at '" + token + "'");

			StringBuffer valueBuffer = new StringBuffer();
			token = tokenIterator.next();

			if (!token.equals("'")) {
				token = tokenIterator.previous().trim();
				throw new CommandCreationExeption(
						"Error 0100:     syntax error at '" + token + "'");

			}

			token = tokenIterator.next();
			while (token.equals("'")) {
				valueBuffer.append(token);
				token = tokenIterator.next();
			}

			keyValuePairs.put(key, valueBuffer.toString());

			/*
			 * if we have no more tokens. it is valid.. and we should
			 * break here.
			 */
			if (!tokenIterator.hasNext())
				break;

			token = tokenIterator.next();
		} while (token.matches("\\s,\\s"));
	}

	@Override
	public ArrayList<Object> execute() {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		ArrayList<Object> retVal = new ArrayList<Object>();
		// TODO add filtering
		List<String> columns = new ArrayList<String>(keyValuePairs.keySet());

		logger.debug("Getting table: " + table);
		logger.debug("Getting column data: " + columns);

		tmsr.getDataBase().getTable(table).preTableAccess(null);

		/*
		 * Do the udpate database work.
		 */
		for (int x = 0; x < tmsr.getDataBase().getTable(table).size(); x++) {
			IDBRow row = tmsr.getDataBase().getTable(table).get(x);

			StringBuffer buff = new StringBuffer();
			for (int y = 0; y < keyValuePairs.size(); y++) {
				buff.append(row.put(columns.get(y), keyValuePairs.get(columns
						.get(y))));
				if (y < columns.size() - 1)
					buff.append("|");
			}
			retVal.add(buff.toString());
		}

		return retVal;
	}

	@Override
	public String toCommandString() {
		// TODO Auto-generated method stub
		return command;
	}

}
