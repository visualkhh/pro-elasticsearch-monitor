package com.khh.esm.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Hit {
    String _index; // : "mindcare_ceragem_care",
    String _type; // : "care",
    String _id; // : "dNvaSnIBXa6CJU7vFAmk",
    Double _score; //: 0.074107975,
    MindCare _source;
    Sort _sort;
}
