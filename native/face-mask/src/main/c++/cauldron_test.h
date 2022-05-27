#pragma once

#include "video_processor2.h"
//#include "video_processor.h"

#include "opencv2/core/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/objdetect.hpp"


// detection of gcc
#ifdef __GNUC__
#define DLLEXPORT
#endif
// detection of mingw (windows)
#ifdef __MINGW64__
#define DLLEXPORT __declspec(dllexport)
#endif
#ifdef _MSC_VER 
#define DLLEXPORT __declspec(dllexport)
#endif 
//default
#ifndef DLLEXPORT
#define DLLEXPORT
#endif 

//TODO rotation.. rotate/detect/unrotate.
#define ROTATION_ENABLED "rotationEnabled"
#define ROTATION_DEGREE "rotationDegree"
#define CASCADE_FILE  "cascade_sheet"
#define MASK_IMAGE  "mask_image"

static int i420_TYPE = ('I') | ('4' << 8) | ('2' << 16) | ('0' << 24);

using namespace cv;
//VISIBLE_CLASSIFIER

// publis the mask on with alpha. requires PNG files. 
static void OverlayImage(Mat* src, Mat* overlay, const Point& location);

extern "C" {
	/*
	* uint32_t GetGuid();
	* CVideoProcessModule* CreateVideoProcessor();
	* void DestroyVideoProcessor(CVideoProcessModule* processor);
	*/
	class TestProcessor :public CVideoProcessModule2
	{
		//return fourCC
		uint32_t get_guid();
		//called when video stream size is first discovered.
		//Type of processing behavior is signaled. 
		uint32_t open(uint16_t width, uint16_t height, uint8_t *type, uint8_t *timing, uint8_t *format, uint8_t *return_type);
		
		void set_env(void* env);
		uint32_t set_audio(uint32_t rate, uint32_t channel_count);
		//resolution change midstream. re-allocate assets. 
		uint32_t reinit(uint16_t width, uint16_t height);
		//apply a key/value property to the processor. 
		uint32_t apply(const char* key, const char* value);
		//called with decoded image. Do a transform in-place to the data pointer.
		//Return 0 if there is no frame available yet.
		//Change the time value if the output time differs from the input time. 
		uint32_t process(uint32_t type, uint8_t *data, uint32_t size, uint32_t* time);
		//free resources.
		uint32_t close();
		void rotate(Mat& img);
		//internal methods.
		void detectAndDraw(Mat& img);

		//cascade .xml
		cv::String face_cascade_name;

		Mat mask;
		uint32_t  counter = 0;

		// Mask offset properties
		float yOffset = 0; // in pixels and appears to cover the additional space needed by the forehead
		float xOffset = 0; // not needed during face detection, but will offset the xPos
						   // Mask scaling properties
		float maskScaleX = 1.2;
		float maskScaleY = 1.6;
		float rotationDegree = 0.0;

		cv::CascadeClassifier face_cascade;
		uint16_t width;
		uint16_t height;
		uint8_t type;
		uint8_t timing;
		uint8_t format;
		uint8_t return_type;
		uint32_t errors;
		uint32_t do_detect;
		uint32_t do_rotation;		
		uint32_t top;
		uint32_t bottom;
		uint32_t left;
		uint32_t right;
		uint32_t has_face;;
	};

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
}