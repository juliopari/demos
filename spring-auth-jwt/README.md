## Introducci�n
Una de las tendencias en el desarrollo web moderno es tener un API RESTful como back-end y como front-end una aplicaci�n desarrollada en Angular 2, adem�s que mediante un API RESTful podemos desarrollar tambi�n aplicaciones m�viles que utilicen estos mismos servicios.

La autenticaci�n entre el API RESTful y sus consumidores es conveniente realizarla mediante tokens, espec�ficamente usando el est�ndar [JSON Web Token](https://tools.ietf.org/html/rfc7519). La autenticaci�n basada en tokens proporciona varias ventajas de las cuales no hablaremos aqu�.

El siguiente diagrama muestra el flujo general de un proceso de autenticaci�n basada en token.

![Image of juliopari](https://raw.githubusercontent.com/juliopari/demos/master/spring-auth-jwt/images/auth_jwt.png)  

1. El cliente env�a sus credenciales (usuario y password) al servidor.
1. Si las credenciales son v�lidas, el servidor devuelve al cliente un token de acceso.
1. El cliente solicita un recurso protegido. En la petici�n, se env�a el token de acceso.
1. El servidor valida el token y en caso de ser v�lido, devuelve el recurso solicitado.

---

## JSON Web Token
[JWT](https://jwt.io/introduction/) consta de 3 partes separadas por un punto ( . )

- Header
- Payload
- Signature

Cada una de estas partes se codifica en base64 de tal forma que el token generado tiene una apariencia como esta,
	
	eyJhbGciOiJIUzUxMiJ9
	.eyJzdWIiOiJqdWxpb3BhcmkiLCJleHAiOjE1MTc2OTgyMjl9
	.Bj-hNJXavrrGZ1Z5HH9gxj7pH9ulYJqCnYkYq0brJIFGbrhyt7CMOa6oAM1p3F1Sp5JH3hiVfNaqC7Xju9aOMw
    
#### Header
El header consta de dos partes, el tipo de token y el algoritmo de hash.

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Si al JSON anterior lo codificamos en base64 tendremos nuestra primer parte del JWT.

#### Payload
El payload contiene datos como: iss (issuer), exp (expiration time) y sub  (subject)

- **iss** es quien emiti� el token
- **exp** contiene la fecha de expiraci�n del token
- **sub** indica el usuario del token

Adem�s podemos indicar otros campos como el nombre, roles, etc.

```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "admin": true,
  "exp": "1425390142"
}
```

## Creaci�n del proyecto con NetBeans 8.2 y Maven

![Image](https://raw.githubusercontent.com/juliopari/demos/master/spring-auth-jwt/images/nb1.png)

![Image](https://raw.githubusercontent.com/juliopari/demos/master/spring-auth-jwt/images/nb2.png)

![Image](https://raw.githubusercontent.com/juliopari/demos/master/spring-auth-jwt/images/nb3.png)

## Pruebas con Postman

![Image](https://raw.githubusercontent.com/juliopari/demos/master/spring-auth-jwt/images/test1.png)

![Image](https://raw.githubusercontent.com/juliopari/demos/master/spring-auth-jwt/images/test2.png)

Al codificar el json anterior en base 64, obtenemos la segunda parte de nuestro JWT. Para m�s informaci�n recomiendo leer el [siguiente enlace](https://jwt.io/introduction/)

---

## Creando la aplicaci�n web
Crearemos un sencillo servicio que devuelva una lista de usuarios

```java
@RestController
public class UsuariosController {

    @GetMapping(path = "/users")
    public List<Usuario> getUsers(){
        return Arrays.asList(new Usuario(1,"Julio"), new Usuario(2,"Cesar"), new Usuario(3, "Juan"));
    }
}
```

Si ejecutamos esta aplicaci�n e ingresamos a http://localhost:8080/users nos pedir� un usuario y contrase�a. Esto es as� porque Spring Boot otorga una configuraci�n de seguridad por defecto. El usuario por defecto es: user

La contrase�a la podremos ver en la consola

    Using default security password: 8b22178d-8caa-4121-a525-9c551ffdfdb6

---

## Seguridad con JWT
En este punto nuestro servicio **/users** est� expuesto a todo mundo. Necesitamos agregar la capa de seguridad, para ello incluimos las siguientes dependencias a nuestro archivo build.gradle (o POM.xml en caso de maven)

```xml
<dependency>
	<groupId>io.jsonwebtoken</groupId>
	<artifactId>jjwt</artifactId>
	<version>0.6.0</version>
</dependency>
```
		
Ahora definiremos las reglas de seguridad mediante una clase a la que llamaremos ``SecurityConfig``

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeRequests()
            .antMatchers("/login").permitAll() //permitimos el acceso a /login a cualquiera
            .anyRequest().authenticated() //cualquier otra peticion requiere autenticacion
            .and()
            // Las peticiones /login pasaran previamente por este filtro
            .addFilterBefore(new LoginFilter("/login", authenticationManager()),
                    UsernamePasswordAuthenticationFilter.class)
                
            // Las dem�s peticiones pasar�n por este filtro para validar el token
            .addFilterBefore(new JwtFilter(),
                    UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // Creamos una cuenta de usuario por default
        auth.inMemoryAuthentication()
                .withUser("juliopari")
                .password("123456")
                .roles("ADMIN");
    }
}
```

Note que ``LoginFilter`` y ``JwtFilter`` son clases que nosotros debemos crear y tendr�n la funci�n de filtros.

``LoginFilter``se encargar� de interceptar las peticiones que provengan de **/login** y obtener el username y password que vienen en el body de la petici�n. 

```java
public class LoginFilter extends AbstractAuthenticationProcessingFilter {

    public LoginFilter(String url, AuthenticationManager authManager) {
        super(new AntPathRequestMatcher(url));
        setAuthenticationManager(authManager);
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest req, HttpServletResponse res)
            throws AuthenticationException, IOException, ServletException {

        // obtenemos el body de la peticion que asumimos viene en formato JSON
        InputStream body = req.getInputStream();

        // Asumimos que el body tendr� el siguiente JSON  {"username":"ask", "password":"123"}
        // Realizamos un mapeo a nuestra clase User para tener ahi los datos
        User user = new ObjectMapper().readValue(body, User.class);

        // Finalmente autenticamos
        // Spring comparar� el user/password recibidos
        // contra el que definimos en la clase SecurityConfig
        return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.emptyList()
                )
        );
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest req,
            HttpServletResponse res, FilterChain chain,
            Authentication auth) throws IOException, ServletException {

        // Si la autenticacion fue exitosa, agregamos el token a la respuesta
        JwtUtil.addAuthentication(res, auth.getName());
    }
}

class User {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

Los comentarios en el c�digo explican por si solo su funcionamiento. Lo que hemos hecho hasta aqu� es definir nuestras reglas de acceso en la clase SecurityConfig e indicamos un user/password por default que se carga en memoria, para peque�os servicios esto est� bien, sin embargo en casos m�s cr�ticos tendremos m�s usuarios y deber�amos almacenarlos en una base de datos. En otro cookbook explicar� como hacer esto, por lo pronto nos bastar� tener un �nico usuario cargado en memoria. 

Cuando llega una petici�n **/login** nuestro filtro ``LoginFilter`` se encargar� de validar las credenciales y en caso de ser v�lidas, crear� un JWT y se enviar� de regreso al cliente. A partir de aqu� el cliente deber� enviar este mismo token al servidor cada vez que solicite recursos protegidos. Podemos observar que tenemos una clase de utilidad llamada ``JwtUtil`` la cu�l usamos para crear el token.

```java
public class JwtUtil {

    // M�todo para crear el JWT y enviarlo al cliente en el header de la respuesta
    static void addAuthentication(HttpServletResponse res, String username) {

        String token = Jwts.builder()
            .setSubject(username)
                
            // Vamos a asignar un tiempo de expiracion de 1 minuto
            // solo con fines demostrativos en el video que hay al final
            .setExpiration(new Date(System.currentTimeMillis() + 60000))
            
            // Hash con el que firmaremos la clave
            .signWith(SignatureAlgorithm.HS512, "limaperu")
            .compact();

        //agregamos al encabezado el token
        res.addHeader("Authorization", "Bearer " + token);
    }

    // M�todo para validar el token enviado por el cliente
    static Authentication getAuthentication(HttpServletRequest request) {
        
        // Obtenemos el token que viene en el encabezado de la peticion
        String token = request.getHeader("Authorization");
        
        // si hay un token presente, entonces lo validamos
        if (token != null) {
            String user = Jwts.parser()
                    .setSigningKey("limaperu")
                    .parseClaimsJws(token.replace("Bearer", "")) //este metodo es el que valida
                    .getBody()
                    .getSubject();

            // Recordamos que para las dem�s peticiones que no sean /login
            // no requerimos una autenticacion por username/password 
            // por este motivo podemos devolver un UsernamePasswordAuthenticationToken sin password
            return user != null ?
                    new UsernamePasswordAuthenticationToken(user, null, emptyList()) :
                    null;
        }
        return null;
    }
}
```

Nuevamente los comentarios en el c�digo explican su funcionamiento.

Finalmente implementamos nuestro segundo filtro. Este filtro tendra como funci�n "validar" el token proporcionado por el cliente. Pongo entre comillas validar puesto que esta tarea no la har� propiamente el filtro, sino que usar� nuestra clase de utilidad ``JwtUtil`` 

```java
/**
 * Las peticiones que no sean /login pasar�n por este filtro
 * el cu�l se encarga de pasar el "request" a nuestra clase de utilidad JwtUtil
 * para que valide el token.
 */
public class JwtFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain filterChain)
            throws IOException, ServletException {


        Authentication authentication = JwtUtil.getAuthentication((HttpServletRequest)request);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request,response);
    }
}
```

---

## Diagrama general

El siguiente diagrama muestra de forma resumida lo que tenemos. Para una mejor comprensi�n ve el video demostrativo para ver el proyecto en funcionamiento.

![Image of juliopari](https://raw.githubusercontent.com/juliopari/demos/master/spring-auth-jwt/images/jwt_spring.png)  

<iframe width="560" height="315" src="#" frameborder="0" allowfullscreen></iframe>
