#pragma once

#include <iostream>

#include "cauldron_test.h"





//Factory Methods the shared library exports.

extern "C"
{
	DLLEXPORT uint32_t GetGuid()
	{
		return 'M' | ('A' << 8) | ('S' << 16) | ('K' << 24);
	}



	DLLEXPORT CVideoProcessModule* CreateVideoProcessor()
	{
		std::cout << "Create facemask processor \n";
		return new TestProcessor();
	}



	DLLEXPORT void DestroyVideoProcessor(CVideoProcessModule* processor)
	{
		std::cout << "Destroy facemask processor \n";
		delete processor;
	}
}




uint32_t TestProcessor::get_guid() {
	return 'M' | ('A' << 8) | ('S' << 16) | ('K' << 24);
}






uint32_t TestProcessor::open(uint16_t width, uint16_t height, uint8_t *type, uint8_t *timing, uint8_t *format, uint8_t *return_type)
{ 

	if (*type != PROCESS_TYPE_ENCODE) {
		std::cout << "Wrong PROCESS_TYPE, using PROCESS_TYPE_ENCODE\n";
	}
	if (*timing != PROCESS_TIMING_WAIT) {
		std::cout << "Wrong PROCESS_TIMING, "<< *timing  <<" using PROCESS_TIMING_WAIT\n";
		printf("%d\n", *timing);
	}
	if (*format != PROCESS_IMAGE_FORMAT_YV420P) {
		std::cout << "Wrong  PROCESS_IMAGE_FORMAT, using PROCESS_IMAGE_FORMAT_YV420P\n";
	}
	if (*return_type != PROCESS_RETURN_IMAGE) {
		std::cout << "Wrong  PROCESS_RETURN, using PROCESS_RETURN_IMAGE\n";
	}
	this->width = width;
	this->height = height;
	//override
	this->type = *type = PROCESS_TYPE_ENCODE;
	this->timing = *timing = PROCESS_TIMING_WAIT;
	this->format = *format - PROCESS_IMAGE_FORMAT_YV420P;
	this->return_type = *return_type = PROCESS_RETURN_IMAGE;

	this->errors = 0;	

	return 0;
}





//apply a key/value property to the processor. 
uint32_t TestProcessor::apply(const char* key, const char* value)
{
	std::cout << "apply :"<< key<< "=" << value <<"\n";
	
	if (strcmp(key, CASCADE_FILE) == 0)
	{
		
		this->face_cascade_name = cv::String(value); 
		if (!face_cascade.load(face_cascade_name)) 
		{		
			this->errors = 1;
			std::cout << "cascade-load fail\n";
		}
		else 
		{
			std::cout << "cascade-load success\n";
		}
	}

	if (strcmp(key, MASK_IMAGE) == 0)
	{		
		mask_mat = imread(value, -1);
		if (mask_mat.data)
		{
			std::cout<<"PNG-load success\n";
		}
		else
		{
			this->errors = 1;
			std::cout << "PNG-load fail\n";
		}
	}

	return 0;
}




//called with decoded image. Do a transform in-place to the data pointer.
//Return 0 if there is no frame available yet.
//Change the time value if the output time differs from the input time. 
uint32_t TestProcessor::process(uint8_t *data, uint32_t size, uint32_t* time)
{
	//packet yv420 planer, cols w,w/4,w/4 rows h, h/4, h/4
	cv::Mat myuv(height + height / 2, width, CV_8UC1, data);
	uint8_t *dest = (uint8_t*)malloc(height * width * 4);
	cv::Mat mbgr(height, width, CV_8UC4, dest);
	cv::cvtColor(myuv, mbgr, CV_YUV2BGRA_I420);

	// TODO rotate for facedetection
	//if ( this->rotationDegree != 0.0f) {
	//	rotate(mbgr);
	//}

	detectAndDraw(mbgr);

	cv::cvtColor(mbgr, myuv, CV_BGRA2YUV_I420);
	
	free(dest);

	return 1;//image returned
}



void TestProcessor::rotate(Mat& src)
{
	cv::Mat dst;
	cv::Point2f ptCp(src.cols*0.5, src.rows*0.5);
	cv::Mat M = cv::getRotationMatrix2D(ptCp, this->rotationDegree, 1.0);
	cv::warpAffine(src, dst, M, src.size(), cv::INTER_CUBIC); //Nearest is too rough, 
	dst.copyTo(src);
	dst.release();
	M.release();

}



void TestProcessor::detectAndDraw(Mat& img)
{
	if (face_cascade.empty())
	{
		std::cout << "THE FACE CASCADE IS EMPTY\n";
		//nothing we can do.
		return;
	}


	std::vector<Rect> faces;
	//prepare face detect gray
	Mat gray;
	//convert to it     
	cvtColor(img, gray, COLOR_BGRA2GRAY);
	//do detect
	face_cascade.detectMultiScale(gray, faces,
		1.1, 3,
		CASCADE_DO_ROUGH_SEARCH,
		Size(100, 100));


	counter++;

	if (faces.size() > 0)
	{
		for (std::vector<Rect>::iterator r = faces.begin(); r != faces.end(); ++r)
		{
			// Prints every 300 frames whether a face has been detected
			// so not to clutter logs and rolls over counter
			if (has_face == 0)
			{
				printf("--DETECTED FACES %lu\n", faces.size());
			}
			counter = 0;

			// Create our mask with all 0's so that the entire frame is drawn
			//mask = Mat::zeros(img.rows, img.cols, CV_8U); // all 0

			// Scalling offset value
			float yOffsetScaled = yOffset * maskScaleY;
			float xOffsetScaled = xOffset * maskScaleX;

			// Move the scaled rectangle xPosition in order to center it
			float scaledWidth = r->width * maskScaleX; // scale the width
			float scaledWidthDifference = r->width - scaledWidth;
			float hDiff = scaledWidthDifference / 2;
			r->x = r->x + hDiff + xOffsetScaled;

			// Move the scaled rectangle yPosition in order to center it
			int scaledHeight = r->height * maskScaleY; // scale the height
													   // Once we have the scaled hieght, we can get a percentage of that to subtract
													   // from the yPos because we have to account for the forehead
			int foreheadHeight = scaledHeight * .09;
			int scaledHeightDifference = r->height - scaledHeight;
			int vDiff = scaledHeightDifference / 2;
			r->y = r->y + vDiff + yOffsetScaled - foreheadHeight;

			// Set the points for our pending bounding Rect declared immediately below
			top = r->y;
			bottom = scaledHeight;
			left = r->x;
			right = scaledWidth;
			has_face = 1;
			break;
		}
	}

	gray.release();

	if (counter > 40)
	{
		has_face = 0;
	}
	//have face, have mask image, and have alpha
	if (has_face == 1 && mask_mat.data && mask_mat.channels() == 4)
	{
		uint8_t *resized = (uint8_t*)malloc(right * bottom * 4);
		cv::Mat mask_resized(right, bottom, CV_8UC4, resized);
		cv::resize(mask_mat, mask_resized, cv::Size(right, bottom));
		Point p(left, top);
		OverlayImage(&img, &mask_resized, p);
		free(resized);
	}
}






static void OverlayImage(Mat* src, Mat* overlay, const Point& location)
{
	for (int y = max(location.y, 0); y < src->rows; ++y)
	{
		int fY = y - location.y;

		if (fY >= overlay->rows)
			break;

		for (int x = max(location.x, 0); x < src->cols; ++x)
		{
			int fX = x - location.x;

			if (fX >= overlay->cols)
				break;

			double opacity = ((double)overlay->data[fY * overlay->step + fX * overlay->channels() + 3]) / 255;

			for (int c = 0; opacity > 0 && c < src->channels(); ++c)
			{
				unsigned char overlayPx = overlay->data[fY * overlay->step + fX * overlay->channels() + c];
				unsigned char srcPx = src->data[y * src->step + x * src->channels() + c];
				src->data[y * src->step + src->channels() * x + c] = srcPx * (1. - opacity) + overlayPx * opacity;
			}
		}
	}
}






//resolution change midstream. re-allocate assets. 
uint32_t TestProcessor::reinit(uint16_t width, uint16_t height)
{
	std::cout << "reinit \n";
	this->width = width;
	this->height = height;
	has_face = 0;
	return 0;
}




//free resources.
uint32_t TestProcessor::close()
{
	std::cout << "close \n";
	return 0;
}

