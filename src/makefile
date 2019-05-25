CC = javac
JTB = jtb132di.jar
JCC = javacc5.jar
GRM1 = minijava.jj
GRM2 = minijava-jtb.jj
CL = *.class
ST = syntaxtree
VS = visitor
MJV = MiniJavaParseTokenManager.java MiniJavaParser.java MiniJavaParserConstants.java MiniJavaParserTokenManager.java
EXC = ParseException.java 
TK = Token.java TokenMgrError.java JavaCharStream.java

all: compile

compile:
	java -jar $(JTB) -te $(GRM1)
	java -jar $(JCC) $(GRM2)
	$(CC) Main.java

clean:
	rm -rf *~ $(GRM2) $(CL) $(ST) $(VS) $(MJV) $(EXC) $(TK)      
