#pragma once

#include "video_processor2.h"

#include "opencv2/core/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
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
	class TestProcessor :public CVideoProcessModule
	{
		//return fourCC
		uint32_t get_guid();
		//called when video stream size is first discovered.
		//Type of processing behavior is signaled. 
		uint32_t open(uint16_t width, uint16_t height, uint8_t *type, uint8_t *timing, uint8_t *format, uint8_t *return_type);
		//resolution change midstream. re-allocate assets. 
		uint32_t reinit(uint16_t width, uint16_t height);
		//apply a key/value property to the processor. 
		uint32_t apply(const char* key, const char* value);
		//called with decoded image. Do a transform in-place to the data pointer.
		//Return 0 if there is no frame available yet.
		//Change the time value if the output time differs from the input time. 
		uint32_t process(uint8_t *data, uint32_t size, uint32_t* time);
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
		cv::Mat mask_mat;
		uint32_t top;
		uint32_t bottom;
		uint32_t left;
		uint32_t right;
		uint32_t has_face;;
	};
}