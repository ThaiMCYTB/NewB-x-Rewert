#ifndef INSTANCING
  $input v_fogColor, v_worldPos, v_underwaterRainTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>
  uniform vec4 FogAndDistanceControl;
#endif

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

    skyColor = colorCorrection(skyColor);

    gl_FragColor = vec4(skyColor, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
