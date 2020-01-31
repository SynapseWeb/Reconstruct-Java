all: ZoomPanLib.class Reconstruct.jar BezierTracing.jar ZoomPanDraw.jar jiv.jar sha1sum.txt reconstruct

AlignSWiFT.class: AlignSWiFT.java makefile
	javac -nowarn -source 1.6 AlignSWiFT.java

ZoomPanLib.class: ZoomPanLib.java makefile
	javac -nowarn -source 1.6 ZoomPanLib.java

XML_Parser.class: XML_Parser.java makefile
	javac -nowarn -source 1.6 XML_Parser.java

TransformClass.class: TransformClass.java makefile
	javac -nowarn -source 1.6 TransformClass.java

SeriesClass.class: SeriesClass.java makefile
	javac -nowarn -source 1.6 SeriesClass.java

SectionClass.class: SectionClass.java makefile
	javac -nowarn -source 1.6 SectionClass.java

ContourClass.class: ContourClass.java makefile
	javac -nowarn -source 1.6 ContourClass.java

ReconstructDefaults.class: ReconstructDefaults.java makefile
	javac -nowarn -source 1.6 ReconstructDefaults.java

Reconstruct.jar: Reconstruct.java ZoomPanLib.class XML_Parser.class TransformClass.class SeriesClass.class SectionClass.class ContourClass.class ReconstructDefaults.class AlignSWiFT.class makefile
	javac Reconstruct.java
	jar -cfe Reconstruct.jar Reconstruct *.class

reconstruct: Reconstruct.jar makefile
	echo '#!/usr/bin/java -jar' | cat - Reconstruct.jar > reconstruct ; chmod +x reconstruct

ZoomPanDraw.jar: ZoomPanDraw.java ZoomPanLib.java makefile
	javac -nowarn -source 1.6 ZoomPanDraw.java
	jar -cfe ZoomPanDraw.jar ZoomPanDraw *.class

jiv.jar: jiv.java ZoomPanLib.java makefile
	javac -nowarn -source 1.6 jiv.java
	jar -cfe jiv.jar jiv *.class

BezierTracing.jar: BezierTracing.java makefile
	rm -f *.class
	javac -nowarn -source 1.6 BezierTracing.java
	jar -cfe BezierTracing.jar BezierTracing *.class


# Note: Using jar to create a jar file changes the SHA1 of the resulting file due to a time stamp of the manifest ... use zip procedure to fix:
# PlotData.jar: PlotData.java makefile
#	  javac PlotData.java
#	  touch -t 201501260000 *.class
#	  zip -X PlotData.jar META-INF/MANIFEST.MF PlotData.java *.class
#	  rm -f *.class


sha1sum.txt: Reconstruct.jar BezierTracing.jar
	sha1sum Reconstruct.jar > sha1sum.txt
	sha1sum jiv.jar >> sha1sum.txt
	sha1sum BezierTracing.jar >> sha1sum.txt

clean:
	rm -f *.jar
	rm -f *.class
	rm -f *~
	rm -f reconstruct

