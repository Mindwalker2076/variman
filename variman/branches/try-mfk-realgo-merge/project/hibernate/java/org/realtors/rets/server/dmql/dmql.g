/*
 * Variman RETS Server
 *
 * Author: Dave Dribin
 * Copyright (c) 2003-2006, The National Association of REALTORS
 * Distributed under a BSD-style license.  See LICENSE.TXT for details.
 */

header {
package org.realtors.rets.server.dmql;
import java.util.*;
import java.net.URLDecoder;
}

class DmqlParser extends Parser;

options
{
    exportVocab = DMQL;
    defaultErrorHandler = false;
    buildAST = true;
    k = 3;
}

// Make sure to define tokens that are also in any sub-parser, like
// DMQL2 since the tree parser is shared among DMQL dialects.
tokens 
{
    FIELD_NAME;
    LOOKUP_LIST; LOOKUP_OR; LOOKUP_AND; LOOKUP_NOT; LOOKUP;
    STRING_LIST; STRING;
    RANGE_LIST; BETWEEN; GREATER; LESS; PERIOD;
}

{
    private void assertValidField(Token t)
        throws SemanticException
    {
        String fieldName = t.getText();
        if (!mMetadata.isValidFieldName(fieldName)) {
            throw newSemanticException("No such field [" + fieldName + "]", t);
        }
    }

    private boolean isLookupField(String fieldName) {
        return ((mMetadata.getFieldType(fieldName) == DmqlFieldType.LOOKUP)
                || (mMetadata.getFieldType(fieldName) ==
                    DmqlFieldType.LOOKUP_MULTI));
    }

    private boolean isCharacterField(String fieldName) {
        return (mMetadata.getFieldType(fieldName) == DmqlFieldType.CHARACTER);
    }

    private boolean isNumericField(String fieldName) {
        return (mMetadata.getFieldType(fieldName) == DmqlFieldType.NUMERIC);
    }

    private void assertValidLookupValue(AST field, Token t)
        throws SemanticException
    {
        String lookupName = field.getText();
        String lookupValue = t.getText();
        if (!mMetadata.isValidLookupValue(lookupName, lookupValue)) {
            throw newSemanticException("No such lookup value [" +
                                       lookupName + "]: " + lookupValue, t);
        }
    }

    public void setMetadata(DmqlParserMetadata metadata) {
        mMetadata = metadata;
    }

    public void traceIn(String text) throws TokenStreamException {
        if (mTrace) super.traceIn(text);
    }

    public void traceOut(String text) throws TokenStreamException {
        if (mTrace) super.traceOut(text);
    }

    public void setTrace(boolean trace) {
        mTrace = trace;
    }

    private SemanticException newSemanticException(String message, Token t) {
        return new SemanticException(message, t.getFilename(), t.getLine(),
                                     t.getColumn());
    }

    
    private boolean mTrace = false;
    private DmqlParserMetadata mMetadata;
}

query
    : search_condition e:EOF {#e.setText("");}
    ;

search_condition
    : query_clause (p:PIPE^ {#p.setType(OR);} query_clause)*
    ;

query_clause
    : query_element (c:COMMA^ {#c.setType(AND);} query_element)*
    ;

query_element
    : field_criteria
    | LPAREN! search_condition RPAREN!
    ;

field_criteria!
    : LPAREN n:field_name EQUAL v:field_value[#n] RPAREN
        {#field_criteria = #v;}
    ;

field_name
    {Token t;}
    : t=text_token {assertValidField(t);}
    ;

field_value [AST name]
    : {isLookupField(name.getText())}? lookup_list[name]
    | {isCharacterField(name.getText())}? string_list[name]
    | range_list[name]
    ;

range_list [AST name]
    : range[name] (COMMA! range[name])*
        {#range_list = #([RANGE_LIST], name, #range_list);}
    ;

range [AST name]
    : between[name]
    | less[name]
    | greater[name]
    ;

between [AST name]
    : between_period
    | between_number
    ;

between_period
    : period m:MINUS^ {#m.setType(BETWEEN);} period
    ;

between_number
    : number m:MINUS^ {#m.setType(BETWEEN);} number
    ;

less [AST name]
    : less_period
    | less_number;

less_period
    : period m:MINUS^ {#m.setType(LESS);}
    ;

less_number
    : number m:MINUS^ {#m.setType(LESS);}
    ;

greater [AST name]
    : greater_period
    | greater_number
    ;

greater_period
    : period p:PLUS^ {#p.setType(GREATER);}
    ;

greater_number
    : number p:PLUS^ {#p.setType(GREATER);}
    ;

number
    : NUMBER
    | i:INT {#i.setType(NUMBER);}
    ;

period
    : date
    | datetime
    | TIME
    ;

date
    : DATE
    | TODAY
    ;

datetime
    : DATETIME
    | NOW
    ;

lookup_list [AST name]
    : o:lookup_or[name]
    | a:lookup_and[name]
    | n:lookup_not[name]
    ;

lookup_or [AST name]
    : PIPE! l1:lookups[name]
        {#lookup_or = #([LOOKUP_OR, "|"], name, l1);}
    // This is the "implied" OR
    |! l2:lookup[name]
        {#lookup_or = #([LOOKUP_OR, "|"], name, l2);}
    ;

lookup_and [AST name]
    : PLUS! l:lookups[name]
        {#lookup_and = #([LOOKUP_AND, "+"], name, l);}
    ;

lookup_not [AST name]
    : TILDE! l:lookups[name]
        {#lookup_not = #([LOOKUP_NOT, "~"], name, l);}
    ;

lookups [AST name]
    : lookup[name] (COMMA! lookup[name])*
    ;

lookup [AST name]
    { Token t; }
    : t=t:text_token {assertValidLookupValue(name,t); #t.setType(LOOKUP);}
    | n:NUMBER       {assertValidLookupValue(name,n); #n.setType(LOOKUP);}
//    | i:INT          {assertValidLookupValue(name,i); #i.setType(LOOKUP);}
    ;

string_list [AST name]
    : string (COMMA! string)*
        {#string_list = #([STRING_LIST], name, #string_list);}
    ;

string
    : (text)+ (string_wildcard)? {#string = #([STRING], #string);}
    | STAR (text)* STAR          {#string = #([STRING], #string);}
    | QUESTION (text)?           {#string = #([STRING], #string);}
    ;

string_wildcard
	: STAR
	| QUESTION (text)*
	;
	
text
    { Token t; }
    : t=text_token
    ;

text_token returns [Token t]
    { t = null; }
    : txt:TEXT      {t=txt;}
    | or:OR         {t=or;      #or.setType(TEXT);}
    | and:AND       {t=and;     #and.setType(TEXT);}
    | not:NOT       {t=not;     #not.setType(TEXT);}
    | today:TODAY   {t=today;   #today.setType(TEXT);}
    | now:NOW       {t=now;     #now.setType(TEXT);}
    | integer:INT   {t=integer; #integer.setType(TEXT);}
    ;

class DmqlLexer extends Lexer;

options
{
    k = 2;
	testLiterals = false;
}


{
    public void traceIn(String text) throws CharStreamException {
        if (mTrace) super.traceIn(text);
    }

    public void traceOut(String text) throws CharStreamException {
        if (mTrace) super.traceOut(text);
    }

    public void setTrace(boolean trace) {
        mTrace = trace;
    }

    private boolean mTrace = false;
}

LPAREN : '(';
RPAREN : ')';
COMMA : ',';
EQUAL : '=';
STAR : '*';
PLUS : '+';
MINUS : '-';
PIPE : '|';
TILDE : '~';
QUESTION : '?';
SEMI : ';';

protected DATETIME : YMD 'T' HMS;
protected DATE : YMD;
protected TIME : HMS;

// Year-Month-Day
protected
YMD 
    : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT;

// Hour:Minute:Second[.Fraction]
protected
HMS : DIGIT DIGIT ':' DIGIT DIGIT ':' DIGIT DIGIT
        ('.' DIGIT ((DIGIT)? DIGIT)?)?;

protected
DIGIT : ('0' .. '9');

protected
ALPHANUM
    : ('a'..'z' | 'A'..'Z' | DIGIT);

protected
TEXT
	: (ALPHANUM | URL_ENCODED_CHAR)+ 
	{ 
	    String s = $getText;
	    $setText(URLDecoder.decode(s));
	}
	;
    
protected
URL_ENCODED_CHAR
    : '%' HEX_DIGIT HEX_DIGIT ;
    
protected
HEX_DIGIT
    : ('0'..'9' | 'A'..'F' | 'a'..'f')
    ;

protected
NUMBER
    : (DIGIT)+ ('.' (DIGIT)*)* ;

protected
INT
    : (DIGIT)+ ('.' (DIGIT)*)* ;

protected OR : "OR" ;
protected AND : "AND" ;
protected NOT : "NOT" ;
protected TODAY : "TODAY" ;
protected NOW : "NOW" ;

// Since these all basically have overlapping patterns, we need to use
// backtracking to try them in order.
TEXT_OR_NUMBER_OR_PERIOD
    : (YMD 'T') => DATETIME {$setType(DATETIME);}
    | (DATE) => DATE {$setType(DATE);}
    | (TIME) => TIME {$setType(TIME);}
    | (OR) => OR {$setType(OR);}
    | (AND) => AND {$setType(AND);}
    | (NOT) => NOT {$setType(NOT);}
    | (TODAY) => TODAY {$setType(TODAY);}
    | (NOW) => NOW {$setType(NOW);}
    | (INT) => INT {$setType(INT);}
    | (NUMBER) => NUMBER {$setType(NUMBER);}
    | TEXT {$setType(TEXT);}
    ;

STRING_LITERAL
    : '"'! (~'"')* ('"'! '"' (~'"')*)* '"'!;

WS  :   (' ' | '\t' | '\n' {newline();} | '\r')
        { _ttype = Token.SKIP; }
    ;
