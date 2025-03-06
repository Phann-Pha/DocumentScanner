package com.pha.document.scanner.common.documentscanner.common.utils

import com.google.mlkit.vision.text.Text

fun strippingWhiteSpace(mrz: String): String
{
    var fixedString = mrz.replace("\n", "")
    fixedString = fixedString.replace(" ", "")
    fixedString = fixedString.replace(".", "")
    fixedString = fixedString.replace("(", "")
    fixedString = fixedString.replace(")", "")
    fixedString = fixedString.replace("\t", "")
    fixedString = fixedString.replace("«", "<")
    fixedString = fixedString.replace("IDKHM", "\nIDKHM")
    return fixedString
}

fun getRawValue(listBlock: List<Text.TextBlock>): String
{
    var textResult = ""
    for (block in listBlock)
    {
        textResult += "\t\t"
        for (line in block.lines)
        {
            textResult += line.text
        }
        textResult += "\n\n"
    }
    
    return textResult
}