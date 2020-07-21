/*
 * This file is generated by jOOQ.
 */
package elfak.teodora.elastic.jooq.model.tables.pojos;


import java.io.Serializable;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class User implements Serializable {

    private static final long serialVersionUID = 458186267;

    private Integer id;

    public User() {}

    public User(User value) {
        this.id = value.id;
    }

    public User(
        Integer id
    ) {
        this.id = id;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("User (");

        sb.append(id);

        sb.append(")");
        return sb.toString();
    }
}
