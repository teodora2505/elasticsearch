/*
 * This file is generated by jOOQ.
 */
package elfak.teodora.elastic.jooq.model;


import org.jooq.Sequence;
import org.jooq.impl.Internal;


/**
 * Convenience access to all sequences in public
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Sequences {

    /**
     * The sequence <code>public.User_Id_seq</code>
     */
    public static final Sequence<Integer> USER_ID_SEQ = Internal.createSequence("User_Id_seq", Public.PUBLIC, org.jooq.impl.SQLDataType.INTEGER.nullable(false), null, null, null, null, false, null);
}