package com.pha.document.scanner.common.documentscanner.ui.components

import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point

internal class Quadrilateral(val contour: MatOfPoint2f, val points: Array<Point>)