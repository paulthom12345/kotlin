fun main(args: Array<String>) {
    Array<caret>
}

// INVOCATION_COUNT: 2
// WITH_ORDER
// EXIST: { lookupString:"Array", tailText:"<T> (kotlin)" }
// EXIST_JAVA_ONLY: { lookupString:"Array", tailText:" (java.sql)" }

// todo: unify following two after introducing collection type aliases
// EXIST_JAVA_ONLY: { lookupString:"ArrayList", tailText:"<E> (java.util)" }
// EXIST_JS_ONLY: { lookupString:"ArrayList", tailText:"<E> (kotlin.collections)" }





// Developer! Every ancient failure of this test is important!