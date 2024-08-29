.PHONY : clean

JAVA_HOME = $(shell dirname $$(readlink -f $$(which java))|sed 's^jre/bin^^')


INCLUDES += '-I$(JAVA_HOME)/include' 
INCLUDES += '-I$(JAVA_HOME)/include/linux'
INCLUDES += '-I../include/amd64-Linux-gpp'


CFLAGS= -fPIC -shared -g $(INCLUDES)
CXXFLAGS += -fpermissive $(INCLUDES) -shared -fPIC

SOURCES = $(shell echo *.cpp)
HEADERS = $(shell echo *.h)
OBJECTS=$(SOURCES:.cpp=.o)

LDFLAGS+= '-L../lib/amd64-Linux-gpp'
LDFLAGS+= -lopencv_core -lopencv_imgproc -lopencv_objdetect

TARGET=facemask-4.2.0.so

all: $(TARGET)

clean:
	rm -f $(OBJECTS) $(TARGET)

$(TARGET) : $(OBJECTS)
	g++ $(CFLAGS) $(OBJECTS) -o $@ $(LDFLAGS)