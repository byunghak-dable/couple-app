#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_org_personal_coupleapp_CustomCameraActivity_convertRGB(JNIEnv *env, jobject thiz, jlong mat_addr_input,
                                                  jlong mat_addr_result, int filter, int threshold) {
    // TODO: implement ConvertRGB()

    Mat &matInput = *(Mat *) mat_addr_input;
    Mat &matResult = *(Mat *) mat_addr_result;

    switch (filter) {

        case 0:

            cvtColor(matInput, matResult, COLOR_RGBA2mRGBA);
            break;

        case 1:

            cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
            break;

        case 2:

            cvtColor(matInput, matResult, COLOR_RGB2Luv);
            break;

        case 3:

            cvtColor(matInput, matResult, COLOR_RGB2BGR);
            break;

        case 4:

            cvtColor(matInput, matResult, COLOR_RGB2HLS);
            break;

        case 5:

            cvtColor(matInput, matResult, COLOR_RGB2HSV);
            break;

        case 6:

            blur(matInput, matResult, Size(5,5));
            break;

        case 7:

            Canny(matInput, matResult, threshold, threshold);
            break;

        default:

            break;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_org_personal_coupleapp_CustomCameraActivity_overlayImage(JNIEnv *env, jobject thiz, jlong mat_addr_input, jlong mat_addr_image,
                                                    jdouble position_x, jdouble position_y) {

    Mat &background = *(Mat *) mat_addr_input;
    Mat &foreground = *(Mat *) mat_addr_image;
    Point location = Point2d(position_x, position_y);

    // start at the row indicated by location, or at row 0 if location.y is negative.
    for (int y = std::max(location.y, 0); y < background.rows; ++y) {
        int fY = y - location.y; // because of the translation

        // we are done of we have processed all rows of the foreground image.
        if (fY >= foreground.rows)
            break;

        // start at the column indicated by location,

        // or at column 0 if location.x is negative.
        for (int x = std::max(location.x, 0); x < background.cols; ++x) {
            int fX = x - location.x; // because of the translation.

            // we are done with this row if the column is outside of the foreground image.
            if (fX >= foreground.cols)
                break;

            // determine the opacity of the foregrond pixel, using its fourth (alpha) channel.
            double opacity = ((double) foreground.data[fY * foreground.step + fX * foreground.channels() + 3]) / 255.;

            // and now combine the background and foreground pixel, using the opacity,

            // but only if opacity > 0.
            for (int c = 0; opacity > 0 && c < background.channels(); ++c) {
                unsigned char foregroundPx =
                        foreground.data[fY * foreground.step + fX * foreground.channels() + c];
                unsigned char backgroundPx =
                        background.data[y * background.step + x * background.channels() + c];
                background.data[y * background.step + background.channels() * x + c] =
                        static_cast<uchar>(backgroundPx * (1. - opacity) + foregroundPx * opacity);
            }
        }
    }
}