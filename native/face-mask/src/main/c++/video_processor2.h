/*
* https://account.red5pro.com/assets/LICENSE.txt
*/
#ifndef _RED5_PRO_VIDPROC2
#define _RED5_PRO_VIDPROC2

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
class CVideoProcessModule2
{
public:
	virtual ~CVideoProcessModule2() { };
	//return fourCC
	virtual uint32_t get_guid() = 0;
	//called when video stream size is first discovered.
	//Type of processing behavior is signaled. 
	virtual uint32_t open(uint16_t width, uint16_t height, uint8_t *type, uint8_t *timing, uint8_t *format, uint8_t *return_type) = 0;
	//
	virtual void set_env(void* env) = 0;
	//
	virtual uint32_t set_audio(uint32_t rate, uint32_t channel_count) = 0;
	//resolution change midstream. re-allocate assets. 
	virtual uint32_t reinit(uint16_t width, uint16_t height) = 0;
	//apply a key/value property to the processor. 
	virtual uint32_t apply(const char* key, const char* value) = 0;
	//called with decoded image. Do a transform in-place to the data pointer.
	//Return 0 if there is no frame available yet.
	//Change the time value if the output time differs from the input time. 
	virtual uint32_t process(uint32_t type, uint8_t *data, uint32_t size, uint32_t* time) = 0;
	//free resources.
	virtual uint32_t close() = 0;
};


/*
Required library export definitions.
These are the typedefs Red5Pro uses to load video process modules.
*/

// Return unique little endian fourCC  ('D' | ('E'<<8 )| ('M'<<16) | ('O'<<24 )) ;
//typedef uint32_t(*GetGuid)();

// Return new instance of CVideoProcessModule. 
//typedef CVideoProcessModule2* (*CreateVideoProcessor2)();

//Destroy instance of CVideoProcessModule. 
//typedef void(*DestroyVideoProcessor2)(CVideoProcessModule2*);

//struct ProcessorFunctionPointers2
//{
//	GetGuid get_guid;
//	CreateVideoProcessor2 create_video_processor;
//	DestroyVideoProcessor2 destroy_video_processor;
//};

#endif