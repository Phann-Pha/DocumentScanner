package com.pha.document.scanner.common.documentscanner.model

import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point

internal class Quadrilateral(val contour: MatOfPoint2f, val points: Array<Point>)