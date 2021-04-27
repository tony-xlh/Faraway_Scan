package com.dynamsoft.farawayscan;

import android.util.Log;

import com.dynamsoft.dbr.EnumIntermediateResultType;
import com.dynamsoft.dbr.IntermediateResult;
import com.dynamsoft.dbr.LocalizationResult;
import com.dynamsoft.dbr.Point;

public class Utils {

    public static Point[] getResultsPointsWithHighestConfidence(IntermediateResult[] intermediateResults){
        for (IntermediateResult ir:intermediateResults){
            if (ir.resultType== EnumIntermediateResultType.IRT_TYPED_BARCODE_ZONE){
                int maxConfidence=0;
                for (Object result:ir.results)
                {
                    LocalizationResult lr = (LocalizationResult) result;
                    maxConfidence=Math.max(lr.confidence,maxConfidence);
                    Log.d("DBR", "confidence: "+lr.confidence);
                }
                Log.d("DBR", "max confidence: "+maxConfidence);
                for (Object result:ir.results)
                {
                    LocalizationResult lr = (LocalizationResult) result;
                    if (lr.confidence==maxConfidence && maxConfidence>80){
                        return lr.resultPoints;
                    }
                }
            }
        }
        return null;
    }
}
