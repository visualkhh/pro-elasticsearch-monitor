package com.khh.esm.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HitContainer {

    HitTotal total;

    List<Hit> hits;

}
