/*
EduDB is made available under the OSI-approved MIT license.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package net.edudb.page;

import java.io.Serializable;

import net.edudb.engine.Config;
import net.edudb.engine.Utility;
import net.edudb.server.ServerWriter;
import net.edudb.structure.DBRecord;
import net.edudb.structure.Recordable;

public class BinaryPage implements Page, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4813060042690551966L;

	/**
	 * Name of the page.
	 */
	private String name;

	/**
	 * Array holding the records in the page.
	 */
	private Recordable[] records;

	/**
	 * The location in the page where the next record should be placed next.
	 */
	private int nextLocation;

	/**
	 * Used to monitor how many threads are currently using the page. Used by
	 * the buffer manager to allow the page to reside in the pool if it must be
	 * replaced.
	 */
	private int openCount;

	public BinaryPage() {
		this.name = Utility.generateUUID();
		this.records = new DBRecord[Config.pageSize()];
		this.nextLocation = 0;
		this.openCount = 0;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Recordable[] getRecords() {
		return records;
	}

	@Override
	public synchronized void addRecord(Recordable record) {
		if (nextLocation <= Config.pageSize()) {
			records[nextLocation++] = record;
		}
	}

	@Override
	public int capacity() {
		return records.length;
	}

	@Override
	public int size() {
		return nextLocation;
	}

	@Override
	public boolean isFull() {
		return size() >= Config.pageSize();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public void open() {
		openCount++;
	}

	@Override
	public void close() {
		openCount--;
	}

	@Override
	public boolean isOpen() {
		return openCount > 0;
	}
	
	@Override
	public void print() {
		for (int i = 0; i < nextLocation; ++i) {
			ServerWriter.getInstance().writeln(records[i]);
		}
	}

}