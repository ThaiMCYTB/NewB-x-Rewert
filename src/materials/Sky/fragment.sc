#ifndef INSTANCING
  $input v_fogColor, v_worldPos, v_underwaterRainTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>
  uniform vec4 FogAndDistanceControl;
#endif
Here is a modified version of ranzies code. It will look completely different though because I changed most of the logic.
```glsl

float point(vec2 pos) {
  pos = fract(pos) - 0.5;
  return 2.0*dot(pos, pos);
}

float voronoi(vec2 pos) {
  return min(point(pos), point(pos * mat2(-0.8, -0.5, 0.314, 0.8)));
}

float amap(vec2 uv, float t) {
  uv += 0.01*sin(40.0*uv.xy);
  float f = voronoi(uv+0.03*t)*(0.5+0.5*voronoi(0.5*uv + 0.08*t));
  f = smoothstep(0.05, 0.8, f);
  //f *= 0.9 + 0.1*sin(40.0*uv.x+40.0*uv.y - t);
  return f;
}

vec3 aurora(vec3 vdir, float t) {
  vec2 uv = 0.2 * vdir.xz / vdir.y;
  vec3 c;
  const int s = 8;
  const float si = 1.0 / float(s);
  for (int i = 0; i < s; i++) {
      float h = float(i)*si;
      float f = amap(uv, t);
      uv *= 1.1 + 0.04*sin(18.0*uv.x + t)*sin(18.0*uv.y - t);
      vec3 col = mix(vec3(0.0, 1.0, 0.0), vec3(0.0, 0.0, 1.0), h);
      col = mix(col.xyz, col.zxy, 0.5 + 0.5*sin(uv.x-t));
      c += col*f*si;
  }
  c *= smoothstep(0.0, 0.4, vdir.y);
  return 3.0*c;
}

vec3 renderWorld(vec3 vdir, vec3 wpos, float t) {
    // example world
    float g = abs(vdir.y);
    vec3 s = mix(vec3(0.05,0.05,0.1),vec3(0.0,0.0,0.0),g*g);
    if (wpos.y>0.0) { // clouds layer
        vec2 u = 4.0*wpos.xz - 0.1*t;
        vec3 a = aurora(vdir, t);
        s += a;
    } else { // ground layer
        s = mix(vec3(0.0,0.05,0.08),s,pow(1.0-g,16.0));
        s -= 0.5*s*g*float(min(fract(wpos.x),fract(wpos.z))<0.02);
    }
    return s;
}
```

vec3 aurora(vec3 vdir, float t) {
  vec2 uv = 0.2 * vdir.xz / vdir.y;
  vec3 c;
  const int s = 16;
  const float si = 1.0 / float(s);
  for (int i = 0; i < s; i++) {
      float h = float(i)*si;
      float f = amap(uv, t);
      uv *= 1.05 + 0.03*sin(30.0*uv.x + t)*sin(30.0*uv.y - t)*sin(10.0*uv.x);
      vec3 col = mix(vec3(0.0, 1.0, 0.0), vec3(0.0, 0.0, 1.0), h);
      col = mix(col.xyz, col.zxy, 0.5 + 0.5*sin(uv.x-t));
      c += col*f*si;
  }
  c *= smoothstep(0.0, 0.4, vdir.y);
  return 3.0*c;
}

void main() {
  #ifndef INSTANCING
    vec3 viewDir = normalize(v_worldPos);

    nl_environment env;
    env.end = false;
    env.nether = false;
    env.underwater = v_underwaterRainTime.x > 0.5;
    env.rainFactor = v_underwaterRainTime.y;

    nl_skycolor skycol;
    if (env.underwater) {
      skycol = nlUnderwaterSkyColors(env.rainFactor, v_fogColor.rgb);
    } else {
      skycol = nlOverworldSkyColors(env.rainFactor, v_fogColor.rgb);
    }

    vec3 skyColor = nlRenderSky(skycol, env, -viewDir, v_fogColor, v_underwaterRainTime.z);
    #ifdef NL_SHOOTING_STAR
      skyColor += NL_SHOOTING_STAR*nlRenderShootingStar(viewDir, v_fogColor, v_underwaterRainTime.z);
    #endif
    #ifdef NL_GALAXY_STARS
      skyColor += NL_GALAXY_STARS*nlRenderGalaxy(viewDir, v_fogColor, env, v_underwaterRainTime.z);
    #endif
skyColor += aurora(viewDir, v_underwaterRainTime.z);


    skyColor = colorCorrection(skyColor);

    gl_FragColor = vec4(skyColor, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
