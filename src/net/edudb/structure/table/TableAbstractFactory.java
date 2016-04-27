package net.edudb.structure.table;

public abstract class TableAbstractFactory {
	
	public abstract TableReader getReader(TableFileType fileType);

	public abstract TableWriter getWriter(TableFileType fileType);
}