/*
* https://account.red5pro.com/assets/LICENSE.txt
*/
#ifndef _RED5_PRO_VIDPROC
#define _RED5_PRO_VIDPROC

#pragma once

#include "inttypes.h"

/**
* Base class for live video processing modules.
* Extend this class and compile into a dll/shared object.
* The dll/shared object must export the following 3 symbols unmangled( extern "C"{} )
*
* uint32_t GetGuid();
* CVideoProcessModule* CreateVideoProcessor();
* void DestroyVideoProcessor(CVideoProcessModule* processor);
*/
class CVideoProcessModule
{
public:
	virtual ~CVideoProcessModule() { };
	
	//return fourCC
	virtual uint32_t get_guid() = 0;
	
	//called when video stream size is first discovered.
	//Type of processing behavior is signaled. 
	virtual uint32_t open(uint16_t width, uint16_t height, uint8_t *type, uint8_t *timing, uint8_t *format, uint8_t *return_type) = 0;
	
	//resolution change midstream. re-allocate assets. 
	virtual uint32_t reinit(uint16_t width, uint16_t height) = 0;
	
	//apply a key/value property to the processor. 
	virtual uint32_t apply(const char* key, const char* value) = 0;
	
	//called with decoded image. Do a transform in-place to the data pointer.
	//Return 0 if there is no frame available yet.
	//Change the time value if the output time differs from the input time. 
	virtual uint32_t process(uint8_t *data, uint32_t size, uint32_t* time) = 0;
	
	//free resources.
	virtual uint32_t close() = 0;
};

/*Core process parameters */
//PROCESS_TYPE_ENCODE, see javadocs
enum PROCESS_TYPE {
	PROCESS_TYPE_NONE,
	PROCESS_TYPE_PASS_THROUGH,
	PROCESS_TYPE_DECODE,
	PROCESS_TYPE_ENCODE,
	PROCESS_TYPE_DEMO
};
//PROCESS_TIMING_WAIT, see javadocs
enum PROCESS_TIMING {
	PROCESS_TIMING_NO_WAIT,
	PROCESS_TIMING_WAIT,
};
//PROCESS_IMAGE_FORMAT_YV420P, see javadocs
enum PROCESS_IMAGE_FORMAT {
	PROCESS_IMAGE_FORMAT_YV420P,
	PROCESS_IMAGE_FORMAT_BGR24,
	PROCESS_IMAGE_FORMAT_BGR32
};
//PROCESS_RETURN_IMAGE, see javadocs
enum PROCESS_RETURN {
	PROCESS_RETURN_NONE,
	PROCESS_RETURN_IMAGE,
	PROCESS_RETURN_DATA
};

/*
Required library export definitions.
These are the typedefs Red5Pro uses to load video process modules.
*/

// Return unique little endian fourCC  ('D' | ('E'<<8 )| ('M'<<16) | ('O'<<24 )) ;
typedef uint32_t(*pGetGuid)();

// Return new instance of CVideoProcessModule. 
typedef CVideoProcessModule* (*pCreateVideoProcessor)();

//Destroy instance of CVideoProcessModule. 
typedef void(*pDestroyVideoProcessor)(CVideoProcessModule*);

struct ProcessorFunctionPointers
{
	pGetGuid get_guid;
	pCreateVideoProcessor create_video_processor;
	pDestroyVideoProcessor destroy_video_processor;
};

#endif