package com.pha.document.scanner.common.documentscanner.model

/**
 * valid MRZ is 3lines x 30character with size of 90
 * valid MRZ is 2 lines x 44char/36char with size of 88/72
 * */
class MrzHelper
{
    fun process(textByBlock: String): MrzModel?
    {
        val mrz = checkBlockCompatibility(textByBlock)
        if (mrz != null)
        {
            return MrzModel(mrz)
        }
        return null
    }
    
    private fun checkBlockCompatibility(cipherText: String): String?
    {
        val block = stripWhiteSpace(cipherText)
        var mrzKey = ""
        
        // filter valid MRZ types
        if (block.startsWith("I") || block.startsWith("P"))
        {
            when (block.length)
            {
                90 -> // TD1
                {
                    val mrz = fixOCRInconsistenciesTD1(block)
                    return if (validateTD1Block(mrz)) mrz else null
                }
                88 -> // TD3
                {
                    val mrz = fixOCRInconsistenciesTD3(block)
                    return if (validateTD3Block(mrz)) mrz else null
                }
                in 72 .. 100 -> // handle mrz fixer
                {
                    mrzKey = stripWhiteSpace(block)
                    if (mrzKey.length != 90 || mrzKey.length != 88 || mrzKey.length != 72) return null
                    when (mrzKey.length)
                    {
                        90 ->
                        {
                            mrzKey = fixOCRInconsistenciesTD1(mrzKey)
                            return if (validateTD1Block(mrzKey)) mrzKey else null
                        }
                        
                        88 ->
                        {
                            mrzKey = fixOCRInconsistenciesTD3(mrzKey)
                            return if (validateTD3Block(mrzKey)) mrzKey else null
                        }
                        
                        else ->
                        {
                            return null
                        }
                    }
                }
            }
        }
        
        return mrzKey
    }
    
    private fun containsDigit(string: String?): Boolean
    {
        var containsDigit = false
        if (!string.isNullOrEmpty())
        {
            for (c in string.toCharArray())
            {
                if (Character.isDigit(c).also { containsDigit = it })
                {
                    break
                }
            }
        }
        return containsDigit
    }
    
    private fun stripWhiteSpace(mrzKey: String): String
    {
        var fixedString = mrzKey.replace("\n", "")
        fixedString = fixedString.replace(" ", "")
        return fixedString
    }
    
    private fun fixOCRInconsistenciesTD1(mrzKey: String): String
    {
        val type = mrzKey.substring(0, 5)
        val cutString = mrzKey.substring(5, 14).replace("O", "0")
        var fixedString = type + cutString + mrzKey.substring(14)
        // firebase sometimes sees < as k we replace <k< with <
        fixedString = fixedString.replace(("([<])([a-z])([<])").toRegex(), "<<<")
        // this is reaching a bit, but on position 18(17) sometimes 0 gets replaced by O
        // let's use REGEX to solve this problem for FRO0 to FR00
        fixedString = fixedString.replace(("(F)(R)([O])(0)").toRegex(), "FR00")
        
        return fixedString
    }
    
    private fun fixOCRInconsistenciesTD3(mrzKey: String): String
    {
        val cutString = mrzKey.substring(44, 53).replace("O", "0")
        var fixedString = cutString + mrzKey.substring(53)
        // firebase sometimes sees < as k we replace <k< with <
        fixedString = fixedString.replace(("([<])([a-z])([<])").toRegex(), "<<<")
        
        return fixedString
    }
    
    private fun validateTD1Block(mrzKey: String): Boolean
    {
        if (mrzKey.length != 90) return false
        if (!(mrzKey.startsWith("I") || mrzKey.startsWith("A") || mrzKey.startsWith("C"))) return false
        if (!containsDigit(mrzKey[59].toString())) return false
        if (containsDigit(mrzKey[60].toString())) return false
        return true
    }
    
    private fun validateTD3Block(mrzKey: String): Boolean
    {
        if (mrzKey.length != 88) return false
        if (!mrzKey.startsWith("P")) return false
        if (!containsDigit(mrzKey[53].toString())) return false
        if (containsDigit(mrzKey[63].toString())) return false
        if (containsDigit(mrzKey[71].toString())) return false
        if (containsDigit(mrzKey[86].toString())) return false
        if (containsDigit(mrzKey[87].toString())) return false
        return true
    }
}