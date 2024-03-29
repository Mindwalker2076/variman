//$Id: Oracle9Dialect.java,v 1.19 2004/08/08 08:23:16 oneovthafew Exp $
package net.sf.hibernate.dialect;

import java.sql.Types;

import net.sf.hibernate.cfg.Environment;
import net.sf.hibernate.Hibernate;

/**
 * An SQL dialect for Oracle 9 (uses ANSI-style syntax where possible).
 * @author Gavin King, David Channon
 */
public class Oracle9Dialect extends Dialect {

	public Oracle9Dialect() {
		super();
		registerColumnType( Types.BIT, "number(1,0)" );
		registerColumnType( Types.BIGINT, "number(19,0)" );
		registerColumnType( Types.SMALLINT, "number(5,0)" );
		registerColumnType( Types.TINYINT, "number(3,0)" );
		registerColumnType( Types.INTEGER, "number(10,0)" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar2($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "date" );
		//registerColumnType( Types.VARBINARY, "RAW" );
		registerColumnType( Types.VARBINARY, "long raw" );
		registerColumnType( Types.VARBINARY, 2000, "raw($l)" );
		registerColumnType( Types.NUMERIC, "number(19, $l)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		getDefaultProperties().setProperty(Environment.USE_STREAMS_FOR_BINARY, "true");
		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);

		registerFunction( "abs", new StandardSQLFunction() );
		registerFunction( "sign", new StandardSQLFunction(Hibernate.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "cosh", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "ln", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "sinh", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "stddev", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "tanh", new StandardSQLFunction(Hibernate.DOUBLE) );
		registerFunction( "variance", new StandardSQLFunction(Hibernate.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction() );
		registerFunction( "trunc", new StandardSQLFunction() );
		registerFunction( "ceil", new StandardSQLFunction() );
		registerFunction( "floor", new StandardSQLFunction() );

		registerFunction( "chr", new StandardSQLFunction(Hibernate.CHARACTER) );
		registerFunction( "initcap", new StandardSQLFunction() );
		registerFunction( "lower", new StandardSQLFunction() );
		registerFunction( "ltrim", new StandardSQLFunction() );
		registerFunction( "rtrim", new StandardSQLFunction() );
		registerFunction( "soundex", new StandardSQLFunction() );
		registerFunction( "upper", new StandardSQLFunction() );
		registerFunction( "ascii", new StandardSQLFunction(Hibernate.INTEGER) );
		registerFunction( "length", new StandardSQLFunction(Hibernate.LONG) );

		registerFunction( "to_char", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "to_date", new StandardSQLFunction(Hibernate.TIMESTAMP) );

		registerFunction( "lastday", new StandardSQLFunction(Hibernate.DATE) );
		registerFunction( "sysdate", new NoArgSQLFunction(Hibernate.DATE, false) );
		registerFunction( "uid", new NoArgSQLFunction(Hibernate.INTEGER, false) );
		registerFunction( "user", new NoArgSQLFunction(Hibernate.STRING, false) );

		// Multi-param string dialect functions...
		registerFunction( "concat", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "instr", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "instrb", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "lpad", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "replace", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "rpad", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "substr", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "substrb", new StandardSQLFunction(Hibernate.STRING) );
		registerFunction( "translate", new StandardSQLFunction(Hibernate.STRING) );

		// Multi-param numeric dialect functions...
		registerFunction( "atan2", new StandardSQLFunction(Hibernate.FLOAT) );
		registerFunction( "log", new StandardSQLFunction(Hibernate.INTEGER) );
		registerFunction( "mod", new StandardSQLFunction(Hibernate.INTEGER) );
		registerFunction( "nvl", new StandardSQLFunction() );
		registerFunction( "power", new StandardSQLFunction(Hibernate.FLOAT) );

		// Multi-param date dialect functions...
		registerFunction( "add_months", new StandardSQLFunction(Hibernate.DATE) );
		registerFunction( "months_between", new StandardSQLFunction(Hibernate.FLOAT) );
		registerFunction( "next_day", new StandardSQLFunction(Hibernate.DATE) );

	}

	public String getAddColumnString() {
		return "add";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select " + sequenceName + ".nextval from dual";
	}
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	public String getCascadeConstraintsString() {
		return " cascade constraints";
	}

	public boolean supportsForUpdateNowait() {
		return true;
	}

	public boolean supportsSequences() {
		return true;
	}

	public boolean supportsLimit() {
		return true;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		StringBuffer pagingSelect = new StringBuffer( sql.length()+100 );
		if (hasOffset) {
			pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ");
		}
		else {
			pagingSelect.append("select * from ( ");
		}
		pagingSelect.append(sql);
		if (hasOffset) {
			pagingSelect.append(" ) row_ where rownum <= ?) where rownum_ > ?");
		}
		else {
			pagingSelect.append(" ) where rownum <= ?");
		}
		return pagingSelect.toString();
	}

	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	public boolean useMaxForLimit() {
		return true;
	}

	public boolean supportsForUpdateOf() {
		return false;
	}

	public String getQuerySequencesString() {
		return "select SEQUENCE_NAME from user_sequences";
	}

}








