package zcu.xutil.sql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.util.Arrays;
import java.util.Date;

import javax.sql.rowset.serial.SerialClob;

import zcu.xutil.ToStrBuilder;
import zcu.xutil.sql.NpSQL;
import zcu.xutil.sql.ResultHandler;
import zcu.xutil.sql.handl.BeanRow;


public class DateType {
	Integer id;
	int intType;
	Long   longcls;
	long   longtype;
	Double doublecls;
	double doubletype;
	BigDecimal bigDec;
	BigInteger bigInt;
	Character charcls;
	char    chartype;
	boolean booltype;
	Boolean boolcls;
	byte    bytetype;
	Byte   bytecls;
	byte[]  bytes;
	char[]  chars;
	SerialClob  clob;
	Blob    blob;
	DBType enumtype;

	Date time=new Date();
	public DateType(){
		//do nothing
	}

	public DateType(Integer id, int intType, Long longcls, long longtype, Double doublecls, double doubletype, BigDecimal bigDec, BigInteger bigInt, Character charcls, char chartype, boolean booltype, Boolean boolcls, byte bytetype, Byte bytecls, byte[] bytes, char[] chars, SerialClob clob, Blob blob, DBType enumtype) {
		this.id = id;
		this.intType = intType;
		this.longcls = longcls;
		this.longtype = longtype;
		this.doublecls = doublecls;
		this.doubletype = doubletype;
		this.bigDec = bigDec;
		this.bigInt = bigInt;
		this.charcls = charcls;
		this.chartype = chartype;
		this.booltype = booltype;
		this.boolcls = boolcls;
		this.bytetype = bytetype;
		this.bytecls = bytecls;
		this.bytes = bytes;
		this.chars = chars;
		this.clob = clob;
		this.blob = blob;
		this.enumtype = enumtype;

	}

	public BigDecimal getBigDec() {
		return bigDec;
	}

	public void setBigDec(BigDecimal bigDec) {
		this.bigDec = bigDec;
	}

	public BigInteger getBigInt() {
		return bigInt;
	}

	public void setBigInt(BigInteger bigInt) {
		this.bigInt = bigInt;
	}

	public Blob getBlob() {
		return blob;
	}

	public void setBlob(Blob blob) {
		this.blob = blob;
	}

	public Boolean getBoolcls() {
		return boolcls;
	}

	public void setBoolcls(Boolean boolcls) {
		this.boolcls = boolcls;
	}

	public boolean isBooltype() {
		return booltype;
	}

	public void setBooltype(boolean booltype) {
		this.booltype = booltype;
	}

	public Byte getBytecls() {
		return bytecls;
	}

	public void setBytecls(Byte bytecls) {
		this.bytecls = bytecls;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public byte getBytetype() {
		return bytetype;
	}

	public void setBytetype(byte bytetype) {
		this.bytetype = bytetype;
	}

	public Character getCharcls() {
		return charcls;
	}

	public void setCharcls(Character charcls) {
		this.charcls = charcls;
	}

	public char[] getChars() {
		return chars;
	}

	public void setChars(char[] chars) {
		this.chars = chars;
	}

	public char getChartype() {
		return chartype;
	}

	public void setChartype(char chartype) {
		this.chartype = chartype;
	}

	public SerialClob getClob() {
		return clob;
	}

	public void setClob(SerialClob clob) {
		this.clob = clob;
	}

	public Double getDoublecls() {
		return doublecls;
	}

	public void setDoublecls(Double doublecls) {
		this.doublecls = doublecls;
	}

	public double getDoubletype() {
		return doubletype;
	}

	public void setDoubletype(double doubletype) {
		this.doubletype = doubletype;
	}

	public DBType getEnumtype() {
		return enumtype;
	}

	public void setEnumtype(DBType enumtype) {
		this.enumtype = enumtype;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getIntType() {
		return intType;
	}

	public void setIntType(int intType) {
		this.intType = intType;
	}

	public Long getLongcls() {
		return longcls;
	}

	public void setLongcls(Long longcls) {
		this.longcls = longcls;
	}

	public long getLongtype() {
		return longtype;
	}

	public void setLongtype(long longtype) {
		this.longtype = longtype;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}




	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bigDec == null) ? 0 : bigDec.hashCode());
		result = prime * result + ((bigInt == null) ? 0 : bigInt.hashCode());
		result = prime * result + ((boolcls == null) ? 0 : boolcls.hashCode());
		result = prime * result + (booltype ? 1231 : 1237);
		result = prime * result + ((bytecls == null) ? 0 : bytecls.hashCode());
		result = prime * result + Arrays.hashCode(bytes);
		result = prime * result + ((charcls == null) ? 0 : charcls.hashCode());
		result = prime * result + Arrays.hashCode(chars);
		result = prime * result + chartype;
		result = prime * result + ((doublecls == null) ? 0 : doublecls.hashCode());
		long temp;
		temp = Double.doubleToLongBits(doubletype);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((enumtype == null) ? 0 : enumtype.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + intType;
		result = prime * result + ((longcls == null) ? 0 : longcls.hashCode());
		result = prime * result + (int) (longtype ^ (longtype >>> 32));
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DateType))
			return false;
		final DateType other = (DateType) obj;
		if (bigDec == null) {
			if (other.bigDec != null)
				return false;
		} else if (bigDec.compareTo(other.bigDec)!=0)
			return false;
		if (bigInt == null) {
			if (other.bigInt != null)
				return false;
		} else if (!bigInt.equals(other.bigInt))
			return false;
		if (boolcls == null) {
			if (other.boolcls != null)
				return false;
		} else if (!boolcls.equals(other.boolcls))
			return false;
		if (booltype != other.booltype)
			return false;
		if (bytecls == null) {
			if (other.bytecls != null)
				return false;
		} else if (!bytecls.equals(other.bytecls))
			return false;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		if (bytetype != other.bytetype)
			return false;
		if (charcls == null) {
			if (other.charcls != null)
				return false;
		} else if (!charcls.equals(other.charcls))
			return false;
		if (!Arrays.equals(chars, other.chars))
			return false;
		if (chartype != other.chartype)
			return false;
		if (doublecls == null) {
			if (other.doublecls != null)
				return false;
		} else if (!doublecls.equals(other.doublecls))
			return false;
		if (Double.doubleToLongBits(doubletype) != Double.doubleToLongBits(other.doubletype))
			return false;
		if (enumtype == null) {
			if (other.enumtype != null)
				return false;
		} else if (!enumtype.equals(other.enumtype))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (intType != other.intType)
			return false;
		if (longcls == null) {
			if (other.longcls != null)
				return false;
		} else if (!longcls.equals(other.longcls))
			return false;
		if (longtype != other.longtype)
			return false;
		if (time == null) {
			if (other.time != null)
				return false;
		} else if (!time.equals(other.time))
			return false;
		return true;
	}

	public String toString(){
		return new ToStrBuilder(DateType.class)
	 	.append( "id",id)
	 	.append( "intType",intType)
	 	.append( "longcls",longcls)
	 	.append( "longtype",longtype)
	 	.append( "doublecls",doublecls)
	 	.append( "doubletype",doubletype)
	 	.append( "bigDec",bigDec)
	 	.append( "bigInt",bigInt)
	 	.append( "charcls",charcls)
	 	.append( "chartype",chartype)
	 	.append( "booltype",booltype)
	 	.append( "boolcls",boolcls)
	 	.append( "bytetype",bytetype)
	 	.append( "bytecls",bytecls)
	 	.append( "bytes",bytes)
	 	.append( "chars",chars)
	 	.append( "clob",clob)
	 	.append( "blob",blob)
	 	.append( "enumtype",enumtype)
	 	.append( "time",time)
	    .toString();
	}
	public final static ResultHandler<DateType> handler= new BeanRow(DateType.class);
	public final static	String droptable="DROP TABLE t_datetype";
//	public final static	String createtable="CREATE TABLE t_datetype (id INTEGER NOT NULL," +
//	"inttype INTEGER,longcls INTEGER,longtype INTEGER,doublecls DECIMAL(12,5),doubletype DECIMAL(12,5)," +
//	"bigDec DECIMAL(20,2),\"BIGINT\" BIGINT,charcls CHAR,chartype CHAR,booltype SMALLINT,boolcls SMALLINT," +
//	"bytetype SMALLINT,bytecls SMALLINT,bytes VARCHAR(50) FOR BIT DATA ,chars VARCHAR(50),clob CLOB," +
//	"blob BLOB,enumtype VARCHAR(50),time TIMESTAMP,PRIMARY KEY(id))";
	public final static	String createtable="CREATE TABLE t_datetype (id INTEGER NOT NULL," +
	"inttype INTEGER,longcls INTEGER,longtype INTEGER,doublecls DECIMAL(12,5),doubletype DECIMAL(12,5)," +
	"bigDec DECIMAL(20,2),\"BIGINT\" BIGINT,charcls CHAR,chartype CHAR,booltype SMALLINT,boolcls SMALLINT," +
	"bytetype SMALLINT,bytecls SMALLINT,bytes BINARY(100) ,chars VARCHAR(50),clob CLOB," +
	"blob BLOB,enumtype VARCHAR(50),time TIMESTAMP,PRIMARY KEY(id))";
	public final static	String retrieve = "select * from T_DATETYPE where id=?";
	public final static	NpSQL create = new NpSQL(
			"insert into T_DATETYPE values(:id, :intType,:longcls,:longtype,:doublecls,:doubletype,:bigDec,:bigInt,:charcls,:chartype,:booltype,:boolcls,:bytetype,:bytecls,:bytes,:chars,:clob,:blob,:enumtype,:time)");
	public final static	NpSQL update =new NpSQL(
			"update T_DATETYPE set INTTYPE=:intType, LONGCLS=:longcls,LONGTYPE=:longtype,DOUBLECLS=:doublecls,DOUBLETYPE=:doubletype,BIGDEC=:bigDec,\"BIGINT\"=:bigInt,CHARCLS=:charcls,CHARTYPE=:chartype,BOOLTYPE=:booltype,BOOLCLS=:boolcls,BYTETYPE=:bytetype,BYTECLS=:bytecls,BYTES=:bytes,CHARS=:chars,CLOB=:clob,BLOB=:blob,ENUMTYPE=:enumtype,TIME=:time where id=:id");
	public final static	String delete ="delete from T_DATETYPE where id=?";
}
