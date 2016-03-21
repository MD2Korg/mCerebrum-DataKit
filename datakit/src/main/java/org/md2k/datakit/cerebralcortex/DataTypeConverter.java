package org.md2k.datakit.cerebralcortex;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center 
 * - Timothy Hnat <twhnat@memphis.edu>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeBoolean;
import org.md2k.datakitapi.datatype.DataTypeBooleanArray;
import org.md2k.datakitapi.datatype.DataTypeByte;
import org.md2k.datakitapi.datatype.DataTypeByteArray;
import org.md2k.datakitapi.datatype.DataTypeDouble;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeFloat;
import org.md2k.datakitapi.datatype.DataTypeFloatArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.datatype.DataTypeLong;
import org.md2k.datakitapi.datatype.DataTypeLongArray;
import org.md2k.datakitapi.datatype.DataTypeString;
import org.md2k.datakitapi.datatype.DataTypeStringArray;

public class DataTypeConverter {
    public static String DataTypeToString(DataType dt) {
        String temp = "";
        if (dt instanceof DataTypeBoolean) {
            temp = Long.toString(dt.getDateTime());
            temp += ", " + ((DataTypeBoolean) dt).getSample();
        } else if (dt instanceof DataTypeBooleanArray) {
            temp = Long.toString(dt.getDateTime());
            for (Boolean d : ((DataTypeBooleanArray) dt).getSample()) {
                temp += ", " + d;
            }
        } else if (dt instanceof DataTypeByte) {
            temp = Long.toString(dt.getDateTime());
            temp += ", " + ((DataTypeByte) dt).getSample();
        } else if (dt instanceof DataTypeByteArray) {
            temp = Long.toString(dt.getDateTime());
            for (Byte d : ((DataTypeByteArray) dt).getSample()) {
                temp += ", " + d;
            }
        } else if (dt instanceof DataTypeDouble) {
            temp = Long.toString(dt.getDateTime());
            temp += ", " + ((DataTypeDouble) dt).getSample();
        } else if (dt instanceof DataTypeDoubleArray) {
            temp = Long.toString(dt.getDateTime());
            for (Double d : ((DataTypeDoubleArray) dt).getSample()) {
                temp += ", " + d;
            }
        } else if (dt instanceof DataTypeFloat) {
            temp = Long.toString(dt.getDateTime());
            temp += ", " + ((DataTypeFloat) dt).getSample();
        } else if (dt instanceof DataTypeFloatArray) {
            temp = Long.toString(dt.getDateTime());
            for (Float d : ((DataTypeFloatArray) dt).getSample()) {
                temp += ", " + d;
            }
        } else if (dt instanceof DataTypeInt) {
            temp = Long.toString(dt.getDateTime());
            temp += ", " + ((DataTypeInt) dt).getSample();
        } else if (dt instanceof DataTypeIntArray) {
            temp = Long.toString(dt.getDateTime());
            for (Integer d : ((DataTypeIntArray) dt).getSample()) {
                temp += ", " + d;
            }
        } else if (dt instanceof DataTypeLong) {
            temp = Long.toString(dt.getDateTime());
            temp += ", " + ((DataTypeLong) dt).getSample();
        } else if (dt instanceof DataTypeLongArray) {
            temp = Long.toString(dt.getDateTime());
            for (Long d : ((DataTypeLongArray) dt).getSample()) {
                temp += ", " + d;
            }
        } else if (dt instanceof DataTypeString) {
            temp = Long.toString(dt.getDateTime());
            temp += ", " + ((DataTypeString) dt).getSample();
        } else if (dt instanceof DataTypeStringArray) {
            temp = Long.toString(dt.getDateTime());
            for (String d : ((DataTypeStringArray) dt).getSample()) {
                temp += ", " + d;
            }
        } else {
            System.out.println("Unknown Object");
        }
        return temp;
    }
}
