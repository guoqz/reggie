package com.guoqz.reggie.dto;

import com.guoqz.reggie.entity.Setmeal;
import com.guoqz.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
