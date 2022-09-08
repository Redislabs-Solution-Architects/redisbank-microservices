package com.redis.demos.redisbankui;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BiggestSpenders {

    private double[] series;
    private String[] labels; 

    public BiggestSpenders(int size)  {
        this.series = new double[size];
        this.labels = new String[size];
    }

}
