package com.pha.document.scanner.common.documentscanner.model

import android.util.Log
import java.util.Locale

class MrzModel(mrzCode: String)
{
    
    private var mrzType: String = ""
    private var mrzKey: String = ""
    private var idType: String = ""
    private var issuingCountry: String = ""
    private var documentNumber: String = ""
    private var expiration: String = ""
    private var surname: String = ""
    private var givenName: String = ""
    private var gender: String = ""
    private var dateOfBirth: String = ""
    private var nationality: String = ""
    private var optionalInfo: String = ""
    
    init
    {
        mrzType = when (mrzCode.length)
        {
            90 -> "TD1" // TD1: ID card has 3x30
            88 -> "TD3" // TD3: passport has 2x44
            else -> "0"
        }
        buildVariables(mrzCode)
    }
    
    private fun buildVariables(mrzCode: String)
    {
        mrzKey = mrzCode
        when (mrzType)
        {
            "TD1" ->
            {
                // split MRZ into three lines:
                val line1 = mrzCode.substring(0, 30)
                val line2 = mrzCode.substring(30, 60)
                val line3 = mrzCode.substring(60, 90)
                
                if (line1.length == 30)
                {
                    idType = line1.substring(0, 2).replace("<".toRegex(), "")  // get Document Type
                    issuingCountry = line1.substring(2, 5).replace("<".toRegex(), "") // get Issuing Country
                    documentNumber = line1.substring(5, 14).replace("<".toRegex(), "") //get ID Card Number
                    optionalInfo = line1.substring(15, 30).replace("<".toRegex(), "") // get Optional Info
                }
                else
                {
                    throw InvalidMRZCodeException("BAD OCR TYPE $line1")
                }
                
                // handle line 2 here
                if (line2.length == 30)
                {
                    dateOfBirth = line2.substring(0, 6) // Get Date of Birth
                    gender = line2.substring(7, 8).replace("<".toRegex(), "Unspecified") // Get GENDER
                    expiration = line2.substring(8, 14) // Get Expiration Date
                    nationality = line2.substring(15, 18).replace("<".toRegex(), "") // Get Nationality
                }
                else
                {
                    throw InvalidMRZCodeException("BAD OCR $line2")
                }
                
                // Handle Line 3 Here
                if (line3.length == 30)
                {
                    val names = line3.split("<<").toTypedArray()
                    surname = names[0].replace("<".toRegex(), "").trim { it <= ' ' }  // Get SURNAME
                    givenName = names[1].replace("<".toRegex(), " ").trim { it <= ' ' } // GET Given Name
                }
                else
                {
                    throw InvalidMRZCodeException("BAD OCR $line3")
                }
            }
            
            "TD3" ->
            {
                // split MRZ into two lines:
                val line1 = mrzCode.substring(0, 44)
                val line2 = mrzCode.substring(36, 88)
                
                if (line1.length == 44)
                {
                    idType = line1.substring(0, 2).replace("<".toRegex(), "") // get Document Type
                    issuingCountry = line1.substring(2, 5).replace("<".toRegex(), "") // get Issuing Country
                    
                    val names = line1.substring(5, 44).split("<<").toTypedArray()
                    surname = names[0].replace("<".toRegex(), "").trim { it <= ' ' }.uppercase(Locale.ROOT) // Get Surname
                    givenName = names[1].replace("<".toRegex(), " ").trim { it <= ' ' }.uppercase(Locale.ROOT) // GET Given Name
                }
                else
                {
                    throw RuntimeException("BAD OCR TYPE $line1")
                }
                
                // handle line 2 here
                if (line2.length == 44)
                {
                    documentNumber = line2.substring(0, 9).replace("<".toRegex(), "") //get Document Number
                    nationality = line2.substring(10, 13).replace("<".toRegex(), "") // Get Nationality
                    dateOfBirth = line2.substring(13, 19) // Get Date of Birth
                    gender = line2.substring(20, 21).replace("<".toRegex(), "Unspecified") // Get GENDER
                    expiration = line2.substring(21, 27) // Get Expiration Date
                    optionalInfo = line2.substring(27, 42).replace("<".toRegex(), "") // get Optional Info
                }
                else
                {
                    throw InvalidMRZCodeException("BAD OCR $line2")
                }
            }
            
            "0" -> Log.d("12345", "buildVariables: ${mrzCode.length} Invalid MRZ.")
        }
    }
    
    fun getMRZType(): String = mrzType
    
    fun getMRZKey(): String = mrzKey
    
    fun getIssuingCountry(): String = issuingCountry
    
    fun getDocumentNumber(): String = documentNumber
    
    fun getExpirationDate(): String = expiration
    
    fun getSurname(): String = surname
    
    fun getGivenName(): String = givenName
    
    fun getGender(): String = gender
    
    fun getDateOfBirth(): String = dateOfBirth
    
    fun getNationality(): String = nationality
    
    fun getOptionalInfo(): String = optionalInfo
    
    class InvalidMRZCodeException(message: String) : Exception(message)
    {
        private fun main(args: Array<String>)
        {
            throw InvalidMRZCodeException("Error!")            // >>> Exception in thread "main"
        }
    }
}